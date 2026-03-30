package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kanban_cards", indexes = {
        @Index(name = "idx_kanban_cards_column", columnList = "column_id"),
        @Index(name = "idx_kanban_cards_column_pos", columnList = "column_id,position")
})
public class KanbanCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "column_id", nullable = false)
    private KanbanColumn column;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer position = 0;

    @OneToMany(mappedBy = "card", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KanbanCardAttachment> attachments = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public KanbanCard() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KanbanColumn getColumn() {
        return column;
    }

    public void setColumn(KanbanColumn column) {
        this.column = column;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public List<KanbanCardAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<KanbanCardAttachment> attachments) {
        this.attachments = attachments;
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
