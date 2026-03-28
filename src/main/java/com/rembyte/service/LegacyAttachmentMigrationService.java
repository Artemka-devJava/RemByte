package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.OrderAttachment;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class LegacyAttachmentMigrationService {

    private static final Logger log = LoggerFactory.getLogger(LegacyAttachmentMigrationService.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".webm", ".mov", ".m4v");

    private final OrderRepository orderRepository;
    private final OrderAttachmentRepository orderAttachmentRepository;

    @Value("${fixbyte.upload.dir:uploads}")
    private String uploadDir;

    @Value("${fixbyte.upload.migration.enabled:true}")
    private boolean migrationEnabled;

    @Value("${fixbyte.upload.migration.delete-legacy:false}")
    private boolean deleteLegacyAfterMigration;

    public LegacyAttachmentMigrationService(OrderRepository orderRepository,
                                            OrderAttachmentRepository orderAttachmentRepository) {
        this.orderRepository = orderRepository;
        this.orderAttachmentRepository = orderAttachmentRepository;
    }

    public void migrateOnStartup() {
        if (!migrationEnabled) {
            log.info("Legacy migration skipped: fixbyte.upload.migration.enabled=false");
            return;
        }

        Path root = Paths.get(uploadDir, "orders").toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            log.info("Legacy migration skipped: directory not found {}", root);
            return;
        }

        int migrated = 0;
        int skipped = 0;
        int failed = 0;

        try (Stream<Path> orderDirs = Files.list(root)) {
            for (Path orderDir : orderDirs.filter(Files::isDirectory).toList()) {
                Long orderId = parseOrderId(orderDir.getFileName().toString());
                if (orderId == null) {
                    skipped++;
                    continue;
                }

                Optional<Order> maybeOrder = orderRepository.findById(orderId);
                if (maybeOrder.isEmpty()) {
                    log.warn("Legacy migration: order {} not found, directory skipped", orderId);
                    skipped++;
                    continue;
                }

                Order order = maybeOrder.get();
                try (Stream<Path> fileStream = Files.list(orderDir)) {
                    for (Path filePath : fileStream.filter(Files::isRegularFile).toList()) {
                        String storedName = filePath.getFileName().toString();
                        if (orderAttachmentRepository.existsByOrderIdAndStoredName(orderId, storedName)) {
                            skipped++;
                            continue;
                        }

                        try {
                            migrateSingleFile(order, orderId, filePath, storedName);
                            migrated++;
                        } catch (Exception e) {
                            failed++;
                            log.error("Legacy migration failed for orderId={}, file={}: {}",
                                    orderId, storedName, e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Legacy migration failed to scan directory {}: {}", root, e.getMessage());
            return;
        }

        log.info("Legacy migration completed: migrated={}, skipped={}, failed={}, deleteLegacyAfterMigration={}",
                migrated, skipped, failed, deleteLegacyAfterMigration);
    }

    private void migrateSingleFile(Order order, Long orderId, Path filePath, String storedName) throws IOException {
        String extension = extensionOf(storedName);
        OrderAttachment.AttachmentType attachmentType = resolveAttachmentType(extension);
        String contentType = resolveContentType(filePath, extension);

        byte[] content = Files.readAllBytes(filePath);

        OrderAttachment attachment = new OrderAttachment();
        attachment.setOrder(order);
        attachment.setStoredName(storedName);
        attachment.setOriginalName(storedName);
        attachment.setContentType(contentType);
        attachment.setAttachmentType(attachmentType);
        attachment.setContent(content);
        attachment.setCreatedAt(LocalDateTime.now());
        orderAttachmentRepository.save(attachment);

        String url = "/uploads/orders/" + orderId + "/" + storedName;
        attachUrlToOrderIfMissing(order, attachmentType, url);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        if (deleteLegacyAfterMigration) {
            Files.deleteIfExists(filePath);
        }
    }

    private void attachUrlToOrderIfMissing(Order order, OrderAttachment.AttachmentType type, String url) {
        if (type == OrderAttachment.AttachmentType.PHOTO) {
            List<String> items = new ArrayList<>(order.getPhotoUrls());
            if (!items.contains(url)) {
                items.add(url);
                order.setPhotoUrls(items);
            }
            return;
        }

        if (type == OrderAttachment.AttachmentType.VIDEO) {
            List<String> items = new ArrayList<>(order.getVideoUrls());
            if (!items.contains(url)) {
                items.add(url);
                order.setVideoUrls(items);
            }
            return;
        }

        List<String> items = new ArrayList<>(order.getFileUrls());
        if (!items.contains(url)) {
            items.add(url);
            order.setFileUrls(items);
        }
    }

    private String resolveContentType(Path filePath, String extension) throws IOException {
        String contentType = Files.probeContentType(filePath);
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }

        if (IMAGE_EXTENSIONS.contains(extension)) {
            if (".png".equals(extension)) return "image/png";
            if (".gif".equals(extension)) return "image/gif";
            if (".webp".equals(extension)) return "image/webp";
            return "image/jpeg";
        }

        if (VIDEO_EXTENSIONS.contains(extension)) {
            if (".webm".equals(extension)) return "video/webm";
            if (".mov".equals(extension)) return "video/quicktime";
            return "video/mp4";
        }

        if (".pdf".equals(extension)) return "application/pdf";
        if (".txt".equals(extension)) return "text/plain";
        if (".csv".equals(extension)) return "text/csv";
        return "application/octet-stream";
    }

    private OrderAttachment.AttachmentType resolveAttachmentType(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return OrderAttachment.AttachmentType.PHOTO;
        }

        if (VIDEO_EXTENSIONS.contains(extension)) {
            return OrderAttachment.AttachmentType.VIDEO;
        }

        return OrderAttachment.AttachmentType.FILE;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private Long parseOrderId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

