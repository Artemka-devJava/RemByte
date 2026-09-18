package com.rembyte.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Пересжатие/поворот фото при загрузке (см. ImageDownscaler). */
class ImageDownscalerTest {

    @Test
    void leavesNonImagesUntouched() {
        byte[] pdf = "%PDF-1.4 fake".getBytes();
        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(pdf, "application/pdf");
        assertThat(d.content()).isSameAs(pdf);
        assertThat(d.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void leavesGifUntouched() throws Exception {
        byte[] fakeGif = jpegBytes(newImage(3000, 100, Color.RED)); // содержимое неважно, решает Content-Type
        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(fakeGif, "image/gif");
        assertThat(d.content()).isSameAs(fakeGif);
    }

    @Test
    void leavesGarbageBytesUntouched() {
        byte[] garbage = {1, 2, 3, 4, 5};
        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(garbage, "image/jpeg");
        assertThat(d.content()).isSameAs(garbage);
        assertThat(d.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void leavesCompactJpegUntouched() throws Exception {
        byte[] jpeg = jpegBytes(newImage(800, 600, Color.BLUE));
        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(jpeg, "image/jpeg");
        assertThat(d.content()).isSameAs(jpeg); // тот же массив — перекодирования не было
    }

    @Test
    void downscalesOversizedJpeg() throws Exception {
        byte[] jpeg = jpegBytes(newImage(4000, 3000, Color.GREEN));
        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(jpeg, "image/jpeg");

        assertThat(d.content()).isNotSameAs(jpeg);
        assertThat(d.contentType()).isEqualTo("image/jpeg");
        BufferedImage out = readImage(d.content());
        assertThat(Math.max(out.getWidth(), out.getHeight())).isLessThanOrEqualTo(1920);
        // пропорции сохранены (4000x3000 = 4:3)
        assertThat((double) out.getWidth() / out.getHeight())
                .isCloseTo(4.0 / 3.0, org.assertj.core.data.Offset.offset(0.02));
        assertThat(d.content().length).isLessThan(jpeg.length);
    }

    @Test
    void downscalesOversizedPngKeepingAlpha() throws Exception {
        BufferedImage img = new BufferedImage(2500, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 0, 0, 128)); // полупрозрачный — есть альфа-канал
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        byte[] png = pngBytes(img);

        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(png, "image/png");

        assertThat(d.contentType()).isEqualTo("image/png");
        BufferedImage out = readImage(d.content());
        assertThat(Math.max(out.getWidth(), out.getHeight())).isLessThanOrEqualTo(1920);
        assertThat(out.getColorModel().hasAlpha()).isTrue();
    }

    @Test
    void correctsExifOrientation6WithoutResizing() throws Exception {
        // Источник: 40x20 (landscape), белый маркер-блок в левом верхнем углу.
        BufferedImage src = newImage(40, 20, Color.BLACK);
        Graphics2D g = src.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(2, 2, 6, 6); // блок x∈[2,8) y∈[2,8)
        g.dispose();
        byte[] jpeg = withFakeExifOrientation(jpegBytes(src), 6);

        ImageDownscaler.Downscaled d = ImageDownscaler.downscaleIfImage(jpeg, "image/jpeg");
        assertThat(d.content()).isNotSameAs(jpeg); // повернули => не «как есть»

        BufferedImage out = readImage(d.content());
        // Поворот на 90° по часовой: точка источника (x,y) -> (h-y, x), h=20.
        // Центр маркера (5,5) -> (15, 5): правая верхняя часть холста 20x40.
        assertThat(out.getWidth()).isEqualTo(20);
        assertThat(out.getHeight()).isEqualTo(40);
        assertThat(brightness(out, 15, 5)).isGreaterThan(200); // маркер тут
        assertThat(brightness(out, 2, 2)).isLessThan(60);      // фон остался тёмным
        assertThat(brightness(out, 2, 35)).isLessThan(60);     // и тут тоже
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static BufferedImage newImage(int w, int h, Color fill) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(fill);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private static byte[] jpegBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private static byte[] pngBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage readImage(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static int brightness(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int gg = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + gg + b) / 3;
    }

    /**
     * Вставляет минимальный APP1(Exif)-сегмент с тегом Orientation сразу после
     * SOI, не трогая остальной JPEG. {@link ImageIO} и наш сканер маркеров
     * одинаково пропускают незнакомые/лишние сегменты.
     */
    private static byte[] withFakeExifOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {
                'I', 'I', 0x2A, 0x00,                   // little-endian TIFF header
                0x08, 0x00, 0x00, 0x00,                 // смещение IFD0 = 8
                0x01, 0x00,                               // 1 запись в IFD0
                0x12, 0x01,                               // тег 0x0112 = Orientation
                0x03, 0x00,                               // тип SHORT
                0x01, 0x00, 0x00, 0x00,                 // count = 1
                (byte) orientation, 0x00, 0x00, 0x00,   // значение + паддинг
                0x00, 0x00, 0x00, 0x00                  // смещение следующего IFD = 0
        };
        byte[] exifHeader = {'E', 'x', 'i', 'f', 0x00, 0x00};
        int payloadLen = exifHeader.length + tiff.length;
        int segLen = payloadLen + 2;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xD8); // SOI
        out.write(0xFF);
        out.write(0xE1); // APP1
        out.write((segLen >> 8) & 0xFF);
        out.write(segLen & 0xFF);
        out.writeBytes(exifHeader);
        out.writeBytes(tiff);
        out.write(jpeg, 2, jpeg.length - 2); // остальной JPEG без исходного SOI
        return out.toByteArray();
    }
}
