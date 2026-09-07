package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сборный лот — несколько купленных деталей ({@link PartItem}), собранных в один комплект
 * (например, готовый ПК) и продаваемых одной сделкой.
 */
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Entity
@Table(name = "part_lots")
public class PartLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "lot", fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<PartItem> items = new ArrayList<>();

    private Double salePrice;
    private LocalDateTime saleDate;

    @Column(nullable = false, length = 20)
    private String status = "IN_STOCK";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PartLot() {}
    public PartLot(String title) { this.title = title; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<PartItem> getItems() { return items; }

    public Double getSalePrice() { return salePrice; }
    public void setSalePrice(Double salePrice) { this.salePrice = salePrice; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status == null || status.isBlank() ? "IN_STOCK" : status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Себестоимость лота — сумма цен закупки всех входящих деталей. */
    @Transient
    @JsonProperty("costTotal")
    public Double getCostTotal() {
        double sum = 0.0;
        for (PartItem item : items) {
            Double p = item.getPurchasePrice();
            sum += p == null ? 0.0 : p;
        }
        return sum;
    }

    /** Прибыль лота: цена продажи минус себестоимость. null, пока лот не продан. */
    @Transient
    @JsonProperty("profit")
    public Double getProfit() {
        if (salePrice == null) return null;
        return salePrice - getCostTotal();
    }
}
