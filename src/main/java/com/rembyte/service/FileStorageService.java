package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.OrderAttachment;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final OrderRepository orderRepository;
    private final OrderAttachmentRepository orderAttachmentRepository;

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
    private Set<String> allowedFileExtensions = new HashSet<>();

    public FileStorageService(OrderRepository orderRepository,
                              OrderAttachmentRepository orderAttachmentRepository) {
        this.orderRepository = orderRepository;
        this.orderAttachmentRepository = orderAttachmentRepository;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        allowedImageExtensions = parseAllowedExtensions(allowedImageExtensionsRaw);
        allowedVideoExtensions = parseAllowedExtensions(allowedVideoExtensionsRaw);
        allowedFileExtensions = parseAllowedExtensions(allowedFileExtensionsRaw);
    }

    public UploadResult saveOrderAttachments(Long orderId, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Файлы не переданы");
        }
        if (files.length > maxFilesPerRequest) {
            throw new IllegalArgumentException("Слишком много файлов за один запрос");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        List<String> photoUrls = new ArrayList<>();
        List<String> videoUrls = new ArrayList<>();
        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String contentType = normalizeContentType(file.getContentType());
            String safeExt = resolveExtension(file.getOriginalFilename(), contentType);

            boolean isImage = contentType.startsWith("image/");
            boolean isVideo = contentType.startsWith("video/");
            boolean isDocument = isDocumentType(contentType, safeExt);

            if (!isImage && !isVideo && !isDocument) {
                throw new IllegalArgumentException("Недопустимый тип файла: " + file.getOriginalFilename());
            }

            OrderAttachment.AttachmentType attachmentType;
            if (isImage) {
                validateExtension(safeExt, allowedImageExtensions, "фото");
                validateSize(file, maxImageSize, "фото");
                attachmentType = OrderAttachment.AttachmentType.PHOTO;
            } else if (isVideo) {
                validateExtension(safeExt, allowedVideoExtensions, "видео");
                validateSize(file, maxVideoSize, "видео");
                attachmentType = OrderAttachment.AttachmentType.VIDEO;
            } else {
                validateExtension(safeExt, allowedFileExtensions, "документа");
                validateSize(file, maxFileSize, "документа");
                attachmentType = OrderAttachment.AttachmentType.FILE;
            }

            String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID() + safeExt;

            try {
                OrderAttachment attachment = new OrderAttachment();
                attachment.setOrder(order);
                attachment.setStoredName(storedName);
                attachment.setOriginalName(sanitizeOriginalName(file.getOriginalFilename(), storedName));
                attachment.setContentType(contentType.isBlank() ? "application/octet-stream" : contentType);
                attachment.setAttachmentType(attachmentType);
                attachment.setContent(file.getBytes());
                orderAttachmentRepository.save(attachment);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения файла: " + file.getOriginalFilename(), e);
            }

            String publicUrl = buildPublicUrl(orderId, storedName);
            if (attachmentType == OrderAttachment.AttachmentType.PHOTO) {
                photoUrls.add(publicUrl);
            } else if (attachmentType == OrderAttachment.AttachmentType.VIDEO) {
                videoUrls.add(publicUrl);
            } else {
                fileUrls.add(publicUrl);
            }
        }

        return new UploadResult(photoUrls, videoUrls, fileUrls);
    }

    public StoredAttachment loadOrderAttachment(Long orderId, String storedName) {
        OrderAttachment fromDb = orderAttachmentRepository.findByOrderIdAndStoredName(orderId, storedName).orElse(null);
        if (fromDb != null) {
            return new StoredAttachment(
                    fromDb.getContent(),
                    fromDb.getContentType(),
                    fromDb.getOriginalName()
            );
        }

        Path legacyFile = getLegacyPath(orderId, storedName);
        if (!Files.exists(legacyFile)) {
            throw new RuntimeException("Вложение не найдено");
        }

        try {
            String detectedContentType = Files.probeContentType(legacyFile);
            return new StoredAttachment(
                    Files.readAllBytes(legacyFile),
                    detectedContentType == null ? "application/octet-stream" : detectedContentType,
                    storedName
            );
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать вложение", e);
        }
    }

    @Transactional
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

        long removedInDb = orderAttachmentRepository.deleteByOrderIdAndStoredName(orderId, fileName);
        if (removedInDb > 0) {
            return;
        }

        Path legacyPath = getLegacyPath(orderId, fileName);
        if (!Files.exists(legacyPath)) {
            return;
        }

        try {
            Files.delete(legacyPath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось удалить вложение", e);
        }
    }

    private Path getLegacyPath(Long orderId, String fileName) {
        return Paths.get(uploadDir, "orders", String.valueOf(orderId), fileName)
                .toAbsolutePath()
                .normalize();
    }

    private String buildPublicUrl(Long orderId, String storedName) {
        return "/uploads/orders/" + orderId + "/" + storedName;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalName(String originalName, String fallbackName) {
        if (originalName == null || originalName.isBlank()) {
            return fallbackName;
        }
        return originalName.replaceAll("[\\r\\n\\t]+", " ").trim();
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

    public record StoredAttachment(byte[] content, String contentType, String originalName) {
    }
}

