package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Строка заказа — одна позиция (услуга или разовая работа).
 * <p>
 * Хранит снимок названия и цену на момент добавления, поэтому переименование
 * или удаление услуги в справочнике не меняет историю заказов. Ссылка на
 * {@link RepairService} остаётся справочной и может быть пустой (разовая позиция).
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "order"})
@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    /** Справочная услуга. null — разовая позиция, живущая только в этом заказе. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id")
    private RepairService service;

    /** Снимок названия на момент добавления. */
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double unitPrice = 0.0;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    public OrderLine() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public RepairService getService() { return service; }
    public void setService(RepairService service) { this.service = service; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice == null ? 0.0 : unitPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = (quantity == null || quantity < 1) ? 1 : quantity;
    }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder == null ? 0 : sortOrder; }

    /** Удобно для фронтенда: id связанной услуги без раскрытия всего объекта. */
    @JsonProperty("serviceId")
    public Long getServiceId() { return service != null ? service.getId() : null; }

    @Transient
    @JsonProperty("lineTotal")
    public Double getLineTotal() {
        double price = unitPrice == null ? 0.0 : unitPrice;
        int qty = quantity == null ? 1 : quantity;
        return price * qty;
    }
}
