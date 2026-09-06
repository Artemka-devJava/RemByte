package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Entity
@Table(name = "clients")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    private String email;
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Механизм ведения клиентов ────────────────────────────────
    /** INDIVIDUAL — физлицо, COMPANY — организация. */
    @Column(length = 20)
    private String type = "INDIVIDUAL";

    /** Свободные метки через запятую: VIP, оптовик, должник, чёрный список… */
    private String tags;

    /** Откуда пришёл: сарафан, реклама, сайт, повторный… */
    @Column(length = 60)
    private String source;

    /** Предпочтительный канал связи: PHONE / TELEGRAM / WHATSAPP / EMAIL. */
    @Column(length = 20)
    private String preferredChannel;

    /** Реквизиты для организаций (ИНН, договор, условия оплаты) — одним полем. */
    @Column(columnDefinition = "TEXT")
    private String companyDetails;

    /** Дата согласия на обработку персональных данных. */
    private LocalDateTime consentPdnAt;

    /** Дата согласия на рекламные рассылки. */
    private LocalDateTime consentMarketingAt;

    /** Если заполнено — клиент в архиве (не удалён). */
    private LocalDateTime archivedAt;

    public Client() {}
    public Client(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type == null || type.isBlank() ? "INDIVIDUAL" : type; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPreferredChannel() { return preferredChannel; }
    public void setPreferredChannel(String preferredChannel) { this.preferredChannel = preferredChannel; }

    public String getCompanyDetails() { return companyDetails; }
    public void setCompanyDetails(String companyDetails) { this.companyDetails = companyDetails; }

    public LocalDateTime getConsentPdnAt() { return consentPdnAt; }
    public void setConsentPdnAt(LocalDateTime consentPdnAt) { this.consentPdnAt = consentPdnAt; }

    public LocalDateTime getConsentMarketingAt() { return consentMarketingAt; }
    public void setConsentMarketingAt(LocalDateTime consentMarketingAt) { this.consentMarketingAt = consentMarketingAt; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
}
