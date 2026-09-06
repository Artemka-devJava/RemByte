package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Фотография проблемы или устройства клиента, не привязанная к заказу.
 * Хранится в БД (как вложения заказов). Заполняется в т.ч. из мобильного клиента.
 */
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Entity
@Table(name = "client_photos", indexes = @Index(name = "idx_client_photos_client", columnList = "client_id"))
public class ClientPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    /** Необязательная привязка к устройству клиента. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @JsonIgnore
    private ClientDevice device;

    @Column(length = 255, nullable = false)
    private String originalName;

    @Column(length = 150, nullable = false)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(name = "content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    @Column(length = 255)
    private String caption;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ClientPhoto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public ClientDevice getDevice() { return device; }
    public void setDevice(ClientDevice device) { this.device = device; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Transient
    @JsonProperty("deviceId")
    public Long getDeviceId() { return device != null ? device.getId() : null; }

    @Transient
    @JsonProperty("url")
    public String getUrl() {
        return client != null && id != null
                ? "/api/clients/" + client.getId() + "/photos/" + id + "/raw"
                : null;
    }
}
