package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kanban_boards", indexes = {
        @Index(name = "idx_kanban_board_owner", columnList = "owner_username")
})
public class KanbanBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_username", nullable = false, length = 120)
    private String ownerUsername;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public KanbanBoard() {
    }

    public KanbanBoard(String ownerUsername, String name) {
        this.ownerUsername = ownerUsername;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
