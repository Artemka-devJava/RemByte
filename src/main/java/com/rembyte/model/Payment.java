package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payments_order", columnList = "order_id"))
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Не отдаём наружу вместе с платежом — заказ и так известен по URL
    // (/api/orders/{id}/payments), а сериализация полного Order (клиент,
    // строки и т.д.) внутри каждого платежа была бы избыточной.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime paymentDate = LocalDateTime.now();

    public Payment() {}
    public Payment(Order order, Double amount) {
        this.order = order;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}

