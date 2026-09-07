package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Одна купленная деталь ПК (комплектующее), купленная на перепродажу.
 * Может продаваться отдельно или быть частью сборного {@link PartLot}.
 */
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Entity
@Table(name = "part_items")
public class PartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String category;
    private String source;

    @Column(nullable = false)
    private Double purchasePrice = 0.0;

    @Column(nullable = false)
    private LocalDateTime purchaseDate = LocalDateTime.now();

    private Double salePrice;
    private LocalDateTime saleDate;

    @Column(nullable = false, length = 20)
    private String status = "IN_STOCK";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    @JsonIgnore
    private PartLot lot;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PartItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice == null ? 0.0 : purchasePrice; }

    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate == null ? LocalDateTime.now() : purchaseDate; }

    public Double getSalePrice() { return salePrice; }
    public void setSalePrice(Double salePrice) { this.salePrice = salePrice; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status == null || status.isBlank() ? "IN_STOCK" : status; }

    public PartLot getLot() { return lot; }
    public void setLot(PartLot lot) { this.lot = lot; }

    @Transient
    @JsonProperty("lotId")
    public Long getLotId() { return lot != null ? lot.getId() : null; }

    @Transient
    @JsonProperty("lotTitle")
    public String getLotTitle() { return lot != null ? lot.getTitle() : null; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Прибыль по этой детали: цена продажи минус цена закупки. null, пока не продана. */
    @Transient
    @JsonProperty("profit")
    public Double getProfit() {
        if (salePrice == null) return null;
        return salePrice - (purchasePrice == null ? 0.0 : purchasePrice);
    }
}
