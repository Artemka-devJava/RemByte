package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
}

