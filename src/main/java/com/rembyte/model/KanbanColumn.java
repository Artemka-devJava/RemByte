package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kanban_columns", indexes = {
        @Index(name = "idx_kanban_columns_board", columnList = "board_id"),
        @Index(name = "idx_kanban_columns_board_pos", columnList = "board_id,position")
})
public class KanbanColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private KanbanBoard board;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer position = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public KanbanColumn() {
    }

    public KanbanColumn(KanbanBoard board, String name, Integer position) {
        this.board = board;
        this.name = name;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KanbanBoard getBoard() {
        return board;
    }

    public void setBoard(KanbanBoard board) {
        this.board = board;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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

