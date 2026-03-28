package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String status = "NEW";

    @Column(columnDefinition = "TEXT")
    private String deviceDescription;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "order_services",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<RepairService> services = new HashSet<>();

    @Column(nullable = false)
    private Double totalPrice = 0.0;

    @Column(nullable = false)
    private Double paidAmount = 0.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Храним в БД как строки с разделителем; наружу отдаем как JSON-массивы.
    @JsonIgnore
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrlsStorage;

    @JsonIgnore
    @Column(name = "video_urls", columnDefinition = "TEXT")
    private String videoUrlsStorage;

    @JsonIgnore
    @Column(name = "file_urls", columnDefinition = "TEXT")
    private String fileUrlsStorage;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Order() {}
    public Order(String orderNumber, Client client) {
        this.orderNumber = orderNumber;
        this.client = client;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeviceDescription() { return deviceDescription; }
    public void setDeviceDescription(String deviceDescription) { this.deviceDescription = deviceDescription; }

    public Set<RepairService> getServices() { return services; }
    public void setServices(Set<RepairService> services) { this.services = services; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @JsonProperty("photoUrls")
    public List<String> getPhotoUrls() { return parseStoredUrls(photoUrlsStorage); }
    @JsonProperty("photoUrls")
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrlsStorage = joinUrls(photoUrls); }

    @JsonProperty("videoUrls")
    public List<String> getVideoUrls() { return parseStoredUrls(videoUrlsStorage); }
    @JsonProperty("videoUrls")
    public void setVideoUrls(List<String> videoUrls) { this.videoUrlsStorage = joinUrls(videoUrls); }

    @JsonProperty("fileUrls")
    public List<String> getFileUrls() { return parseStoredUrls(fileUrlsStorage); }
    @JsonProperty("fileUrls")
    public void setFileUrls(List<String> fileUrls) { this.fileUrlsStorage = joinUrls(fileUrls); }

    public void addPhotoUrls(List<String> newUrls) {
        if (newUrls == null || newUrls.isEmpty()) return;
        List<String> merged = new ArrayList<>(getPhotoUrls());
        merged.addAll(newUrls);
        setPhotoUrls(merged);
    }

    public void addVideoUrls(List<String> newUrls) {
        if (newUrls == null || newUrls.isEmpty()) return;
        List<String> merged = new ArrayList<>(getVideoUrls());
        merged.addAll(newUrls);
        setVideoUrls(merged);
    }

    public void addFileUrls(List<String> newUrls) {
        if (newUrls == null || newUrls.isEmpty()) return;
        List<String> merged = new ArrayList<>(getFileUrls());
        merged.addAll(newUrls);
        setFileUrls(merged);
    }

    public boolean removeAttachmentUrl(String attachmentUrl) {
        if (attachmentUrl == null || attachmentUrl.isBlank()) return false;

        List<String> photos = new ArrayList<>(getPhotoUrls());
        List<String> videos = new ArrayList<>(getVideoUrls());
        List<String> files  = new ArrayList<>(getFileUrls());

        boolean removedPhoto = photos.removeIf(url -> attachmentUrl.equals(url));
        boolean removedVideo = videos.removeIf(url -> attachmentUrl.equals(url));
        boolean removedFile  = files.removeIf(url -> attachmentUrl.equals(url));

        if (removedPhoto) setPhotoUrls(photos);
        if (removedVideo) setVideoUrls(videos);
        if (removedFile)  setFileUrls(files);

        return removedPhoto || removedVideo || removedFile;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPaymentStatus() {
        if (paidAmount <= 0) return "Не оплачено";
        if (paidAmount < totalPrice) return "Частично оплачено";
        return "Оплачено";
    }

    public Double getBalance() {
        return Math.max(0, totalPrice - paidAmount);
    }

    private List<String> parseStoredUrls(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String joinUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        return urls.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }
}

