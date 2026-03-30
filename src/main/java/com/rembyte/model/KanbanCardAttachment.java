package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "kanban_card_attachments",
        indexes = {
                @Index(name = "idx_kanban_attachment_card", columnList = "card_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kanban_attachment_name", columnNames = {"card_id", "stored_name"})
        }
)
public class KanbanCardAttachment {

    public enum AttachmentType {
        IMAGE,
        TEXT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private KanbanCard card;

    @Column(name = "stored_name", nullable = false, length = 180)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 12)
    private AttachmentType attachmentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public KanbanCardAttachment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KanbanCard getCard() {
        return card;
    }

    public void setCard(KanbanCard card) {
        this.card = card;
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

