package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Фотография детали ({@link PartItem}) — купленного комплектующего.
 * Хранится в БД (как {@link ClientPhoto}).
 */
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Entity
@Table(name = "part_item_photos", indexes = @Index(name = "idx_part_item_photos_item", columnList = "item_id"))
public class PartItemPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    private PartItem item;

    @Column(length = 255, nullable = false)
    private String originalName;

    @Column(length = 150, nullable = false)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(name = "content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PartItemPhoto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PartItem getItem() { return item; }
    public void setItem(PartItem item) { this.item = item; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Transient
    @JsonProperty("itemId")
    public Long getItemId() { return item != null ? item.getId() : null; }

    @Transient
    @JsonProperty("url")
    public String getUrl() {
        return item != null && id != null
                ? "/api/parts/" + item.getId() + "/photos/" + id + "/raw"
                : null;
    }
}
