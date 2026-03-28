package com.rembyte.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${fixbyte.upload.dir:uploads}")
    private String uploadDir;

    @Value("${fixbyte.upload.max-files-per-request:10}")
    private int maxFilesPerRequest;

    @Value("${fixbyte.upload.allowed-image-extensions:.jpg,.jpeg,.png,.webp,.gif}")
    private String allowedImageExtensionsRaw;

    @Value("${fixbyte.upload.allowed-video-extensions:.mp4,.webm,.mov,.m4v}")
    private String allowedVideoExtensionsRaw;

    @Value("${fixbyte.upload.allowed-file-extensions:.txt,.pdf,.doc,.docx,.xls,.xlsx,.csv,.rtf}")
    private String allowedFileExtensionsRaw;

    @Value("${fixbyte.upload.max-image-size:15MB}")
    private DataSize maxImageSize;

    @Value("${fixbyte.upload.max-video-size:120MB}")
    private DataSize maxVideoSize;

    @Value("${fixbyte.upload.max-file-size:20MB}")
    private DataSize maxFileSize;

    private Set<String> allowedImageExtensions = new HashSet<>();
    private Set<String> allowedVideoExtensions = new HashSet<>();
    private Set<String> allowedFileExtensions  = new HashSet<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        allowedImageExtensions = parseAllowedExtensions(allowedImageExtensionsRaw);
        allowedVideoExtensions = parseAllowedExtensions(allowedVideoExtensionsRaw);
        allowedFileExtensions  = parseAllowedExtensions(allowedFileExtensionsRaw);
    }

    public UploadResult saveOrderAttachments(Long orderId, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Файлы не переданы");
        }
        if (files.length > maxFilesPerRequest) {
            throw new IllegalArgumentException("Слишком много файлов за один запрос");
        }

        List<String> photoUrls = new ArrayList<>();
        List<String> videoUrls = new ArrayList<>();
        List<String> fileUrls  = new ArrayList<>();

        Path orderDir = Paths.get(uploadDir, "orders", String.valueOf(orderId)).toAbsolutePath().normalize();
        try {
            Files.createDirectories(orderDir);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку для вложений", e);
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType() == null
                    ? ""
                    : file.getContentType().toLowerCase(Locale.ROOT);

            String safeExt = resolveExtension(file.getOriginalFilename(), contentType);

            boolean isImage = contentType.startsWith("image/");
            boolean isVideo = contentType.startsWith("video/");
            boolean isDocument = isDocumentType(contentType, safeExt);

            if (!isImage && !isVideo && !isDocument) {
                throw new IllegalArgumentException("Недопустимый тип файла: " + file.getOriginalFilename());
            }

            if (isImage) {
                validateExtension(safeExt, allowedImageExtensions, "фото");
                validateSize(file, maxImageSize, "фото");
            } else if (isVideo) {
                validateExtension(safeExt, allowedVideoExtensions, "видео");
                validateSize(file, maxVideoSize, "видео");
            } else {
                validateExtension(safeExt, allowedFileExtensions, "документа");
                validateSize(file, maxFileSize, "документа");
            }

            String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID() + safeExt;
            Path target = orderDir.resolve(storedName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения файла: " + file.getOriginalFilename(), e);
            }

            String publicUrl = "/uploads/orders/" + orderId + "/" + storedName;
            if (isImage) {
                photoUrls.add(publicUrl);
            } else if (isVideo) {
                videoUrls.add(publicUrl);
            } else {
                fileUrls.add(publicUrl);
            }
        }

        return new UploadResult(photoUrls, videoUrls, fileUrls);
    }

    private boolean isDocumentType(String contentType, String ext) {
        if (contentType.startsWith("text/")) return true;
        if (contentType.contains("pdf")) return true;
        if (contentType.contains("msword") || contentType.contains("wordprocessingml")) return true;
        if (contentType.contains("ms-excel") || contentType.contains("spreadsheetml")) return true;
        if (contentType.contains("ms-powerpoint") || contentType.contains("presentationml")) return true;
        if (contentType.contains("octet-stream") || contentType.isEmpty()) {
            return allowedFileExtensions.contains(ext);
        }
        return false;
    }

    public void deleteOrderAttachment(Long orderId, String attachmentUrl) {
        if (attachmentUrl == null || attachmentUrl.isBlank()) {
            throw new IllegalArgumentException("URL вложения не передан");
        }

        String expectedPrefix = "/uploads/orders/" + orderId + "/";
        if (!attachmentUrl.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Некорректный путь вложения");
        }

        String fileName = attachmentUrl.substring(expectedPrefix.length());
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Некорректное имя файла");
        }

        Path target = Paths.get(uploadDir, "orders", String.valueOf(orderId), fileName)
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(target)) {
            return;
        }

        try {
            Files.delete(target);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось удалить вложение", e);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
                if (ext.matches("\\.[a-z0-9]{1,10}")) {
                    return ext;
                }
            }
        }

        if (contentType.contains("jpeg")) return ".jpg";
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("webp")) return ".webp";
        if (contentType.contains("gif")) return ".gif";
        if (contentType.contains("mp4")) return ".mp4";
        if (contentType.contains("webm")) return ".webm";
        if (contentType.contains("quicktime")) return ".mov";
        return ".bin";
    }

    private Set<String> parseAllowedExtensions(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(ext -> !ext.isBlank())
                .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void validateExtension(String ext, Set<String> allowed, String fileTypeLabel) {
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException("Недопустимое расширение для " + fileTypeLabel + ": " + ext);
        }
    }

    private void validateSize(MultipartFile file, DataSize maxSize, String fileTypeLabel) {
        if (file.getSize() > maxSize.toBytes()) {
            throw new IllegalArgumentException(
                    "Слишком большой " + fileTypeLabel + ". Максимум: " + humanReadable(maxSize.toBytes())
            );
        }
    }

    private String humanReadable(long bytes) {
        long mb = bytes / (1024 * 1024);
        if (mb > 0) return mb + "MB";
        long kb = bytes / 1024;
        return Math.max(kb, 1) + "KB";
    }

    public record UploadResult(List<String> photoUrls, List<String> videoUrls, List<String> fileUrls) {
    }
}

