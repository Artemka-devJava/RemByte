package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Стартовый бюджет модуля «Комплектующие» — одна строка настроек.
 * Текущий баланс не хранится: он всегда пересчитывается как
 * startingAmount − сумма закупок + сумма продаж (см. PartsService.getStatistics).
 */
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@Entity
@Table(name = "parts_budget")
public class PartsBudget {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Double startingAmount = 0.0;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PartsBudget() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getStartingAmount() { return startingAmount; }
    public void setStartingAmount(Double startingAmount) { this.startingAmount = startingAmount == null ? 0.0 : startingAmount; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
