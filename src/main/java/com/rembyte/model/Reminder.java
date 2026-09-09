package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Напоминание для оператора: «перезвонить по заявке», «уточнить деталь» и т.п.
 * Может быть привязано к клиенту и/или заказу (простыми id, без связей).
 */
@Entity
@Table(name = "reminders", indexes = {
        @Index(name = "idx_reminders_open", columnList = "done, due_at")
})
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    /** Когда напомнить. null — без конкретной даты. */
    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(nullable = false)
    private boolean done = false;

    @Column(name = "done_at")
    private LocalDateTime doneAt;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public LocalDateTime getDoneAt() { return doneAt; }
    public void setDoneAt(LocalDateTime doneAt) { this.doneAt = doneAt; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
