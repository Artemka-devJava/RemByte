package com.rembyte.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Уменьшает фото перед сохранением в БД (LONGBLOB), чтобы бэкапы и страницы
 * со списками фото не раздувались полноразмерными снимками с камеры
 * телефона (см. OPERATIONS.md, «Резервное копирование»).
 * <p>
 * Учитывает EXIF-ориентацию JPEG — иначе портретные фото с камеры после
 * перекодирования легли бы «набок» (пиксели у камерных JPEG почти всегда
 * физически лежат landscape, поворот задаётся тегом Orientation).
 * <p>
 * На любой сбой, неподдержанный формат (например, WEBP — {@link ImageIO} не
 * читает его без стороннего плагина) или уже компактное изображение —
 * возвращает исходные байты без изменений: аплоад не должен ломаться
 * из-за оптимизации.
 */
final class ImageDownscaler {

    private static final int MAX_DIMENSION = 1920;
    private static final float JPEG_QUALITY = 0.85f;

    private ImageDownscaler() {
    }

    /** Результат: возможно пересжатые байты + содержимое-соответствующий Content-Type. */
    record Downscaled(byte[] content, String contentType) {
    }

    static Downscaled downscaleIfImage(byte[] original, String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (original == null || original.length == 0 || !ct.startsWith("image/") || ct.contains("gif")) {
            return new Downscaled(original, contentType); // GIF (анимация) и не-изображения не трогаем
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
            if (src == null) {
                return new Downscaled(original, contentType); // формат не читается ImageIO — оставляем как есть
            }

            int orientation = (ct.contains("jpeg") || ct.contains("jpg")) ? readJpegOrientation(original) : 1;
            BufferedImage oriented = applyOrientation(src, orientation);

            boolean tooBig = Math.max(oriented.getWidth(), oriented.getHeight()) > MAX_DIMENSION;
            if (!tooBig && oriented == src) {
                return new Downscaled(original, contentType); // уже компактное и без поворота — не теряем качество
            }

            BufferedImage result = tooBig ? scaleDown(oriented, MAX_DIMENSION) : oriented;
            boolean alpha = result.getColorModel().hasAlpha();
            byte[] bytes = alpha ? encodePng(result) : encodeJpeg(result, JPEG_QUALITY);
            return new Downscaled(bytes, alpha ? "image/png" : "image/jpeg");
        } catch (Exception e) {
            return new Downscaled(original, contentType); // на любой сбой — не портим загрузку
        }
    }

    private static BufferedImage scaleDown(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = maxDim / (double) Math.max(w, h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));

        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(nw, nh, type);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    /**
     * Поворачивает/отражает изображение по EXIF-тегу Orientation (1-8).
     * Значения 5 и 7 (transpose/transverse) на практике камерами телефонов
     * не выдаются — намеренно не трогаем такие кадры, чтобы не рисковать
     * неверным поворотом на непроверенной ветке.
     */
    private static BufferedImage applyOrientation(BufferedImage img, int orientation) {
        if (orientation <= 1) {
            return img;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int type = img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        AffineTransform t = new AffineTransform();
        int dw = w;
        int dh = h;
        switch (orientation) {
            case 2 -> { // отражение по горизонтали
                t.translate(w, 0);
                t.scale(-1, 1);
            }
            case 3 -> { // поворот на 180°
                t.translate(w, h);
                t.rotate(Math.PI);
            }
            case 4 -> { // отражение по вертикали
                t.translate(0, h);
                t.scale(1, -1);
            }
            case 6 -> { // поворот на 90° по часовой
                dw = h;
                dh = w;
                t.translate(h, 0);
                t.rotate(Math.PI / 2);
            }
            case 8 -> { // поворот на 90° против часовой
                dw = h;
                dh = w;
                t.translate(0, w);
                t.rotate(-Math.PI / 2);
            }
            default -> {
                return img; // 5, 7, прочее — не трогаем
            }
        }

        BufferedImage dst = new BufferedImage(dw, dh, type);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, t, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    private static byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
        BufferedImage rgb = img;
        if (img.getColorModel().hasAlpha() || img.getType() != BufferedImage.TYPE_INT_RGB) {
            rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.drawImage(img, 0, 0, null);
            } finally {
                g.dispose();
            }
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            return simpleEncode(rgb, "jpg");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgb, null, null), param);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static byte[] encodePng(BufferedImage img) throws IOException {
        return simpleEncode(img, "png");
    }

    private static byte[] simpleEncode(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(img, format, out)) {
            throw new IOException("Нет кодировщика для формата " + format);
        }
        return out.toByteArray();
    }

    /** Минимальный парсер EXIF-ориентации из APP1-сегмента JPEG (тег 0x0112). Без внешних библиотек. */
    private static int readJpegOrientation(byte[] jpeg) {
        try {
            if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
                return 1;
            }
            int pos = 2;
            while (pos + 4 <= jpeg.length) {
                if ((jpeg[pos] & 0xFF) != 0xFF) {
                    break;
                }
                int marker = jpeg[pos + 1] & 0xFF;
                if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                    pos += 2;
                    continue;
                }
                if (marker == 0xD9 || marker == 0xDA) {
                    break; // конец файла / начало данных скана — EXIF (APP1) идёт раньше
                }
                if (pos + 4 > jpeg.length) {
                    break;
                }
                int segLen = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
                if (marker == 0xE1 && segLen >= 8) {
                    int p = pos + 4;
                    if (p + 6 <= jpeg.length && jpeg[p] == 'E' && jpeg[p + 1] == 'x'
                            && jpeg[p + 2] == 'i' && jpeg[p + 3] == 'f') {
                        return parseTiffOrientation(jpeg, p + 6);
                    }
                }
                pos += 2 + segLen;
            }
        } catch (Exception ignored) {
            // повреждённый/нестандартный EXIF — считаем, что ориентация обычная
        }
        return 1;
    }

    private static int parseTiffOrientation(byte[] b, int tiffStart) {
        if (tiffStart + 8 > b.length) {
            return 1;
        }
        boolean bigEndian = b[tiffStart] == 'M';
        int ifdOffset = readInt32(b, tiffStart + 4, bigEndian);
        int ifdStart = tiffStart + ifdOffset;
        if (ifdStart < 0 || ifdStart + 2 > b.length) {
            return 1;
        }
        int entries = readInt16(b, ifdStart, bigEndian);
        for (int i = 0; i < entries; i++) {
            int entryOffset = ifdStart + 2 + i * 12;
            if (entryOffset + 12 > b.length) {
                break;
            }
            int tag = readInt16(b, entryOffset, bigEndian);
            if (tag == 0x0112) {
                int value = readInt16(b, entryOffset + 8, bigEndian);
                return (value >= 1 && value <= 8) ? value : 1;
            }
        }
        return 1;
    }

    private static int readInt16(byte[] b, int off, boolean bigEndian) {
        int b0 = b[off] & 0xFF;
        int b1 = b[off + 1] & 0xFF;
        return bigEndian ? (b0 << 8) | b1 : (b1 << 8) | b0;
    }

    private static int readInt32(byte[] b, int off, boolean bigEndian) {
        int b0 = b[off] & 0xFF;
        int b1 = b[off + 1] & 0xFF;
        int b2 = b[off + 2] & 0xFF;
        int b3 = b[off + 3] & 0xFF;
        return bigEndian ? (b0 << 24) | (b1 << 16) | (b2 << 8) | b3 : (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }
}
