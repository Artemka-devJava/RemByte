package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_attachments",
        indexes = {
                @Index(name = "idx_order_attachments_order", columnList = "order_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_attachment_name", columnNames = {"order_id", "stored_name"})
        }
)
public class OrderAttachment {

    public enum AttachmentType {
        PHOTO,
        VIDEO,
        FILE,
        /** Сгенерированный акт приёма оборудования в ремонт (PDF). */
        ACT
    }

    /**
     * Храним тип строкой через конвертер, а не {@code @Enumerated}, чтобы Hibernate
     * не создавал CHECK-ограничение {@code attachment_type IN (...)}: при добавлении
     * нового значения в enum старое ограничение ломает вставку (ddl-auto=update его
     * не пересоздаёт). Неизвестное значение из БД читается как {@code null}.
     */
    @Converter
    public static class AttachmentTypeConverter
            implements AttributeConverter<AttachmentType, String> {
        @Override
        public String convertToDatabaseColumn(AttachmentType type) {
            return type == null ? null : type.name();
        }

        @Override
        public AttachmentType convertToEntityAttribute(String value) {
            if (value == null || value.isBlank()) return null;
            try {
                return AttachmentType.valueOf(value.trim());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "stored_name", nullable = false, length = 180)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Convert(converter = AttachmentTypeConverter.class)
    @Column(name = "attachment_type", nullable = false, length = 12)
    private AttachmentType attachmentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OrderAttachment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

