package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String siteKey;

    @Column(nullable = false, unique = true, length = 64)
    private String publicToken = UUID.randomUUID().toString();

    @Column(length = 160)
    private String visitorName;

    @Column(length = 64)
    private String visitorPhone;

    @Column(length = 160)
    private String visitorEmail;

    @Column(length = 1000)
    private String visitorPageUrl;

    @Column(length = 255)
    private String visitorOrigin;

    @Column(columnDefinition = "TEXT")
    private String visitorUserAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ChatConversationStatus status = ChatConversationStatus.OPEN;

    @Column(length = 120)
    private String assignedOperatorUsername;

    @Column(nullable = false)
    private Integer unreadForOperator = 0;

    @Column(nullable = false)
    private Integer unreadForVisitor = 0;

    private LocalDateTime lastMessageAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public String getVisitorPhone() {
        return visitorPhone;
    }

    public void setVisitorPhone(String visitorPhone) {
        this.visitorPhone = visitorPhone;
    }

    public String getVisitorEmail() {
        return visitorEmail;
    }

    public void setVisitorEmail(String visitorEmail) {
        this.visitorEmail = visitorEmail;
    }

    public String getVisitorPageUrl() {
        return visitorPageUrl;
    }

    public void setVisitorPageUrl(String visitorPageUrl) {
        this.visitorPageUrl = visitorPageUrl;
    }

    public String getVisitorOrigin() {
        return visitorOrigin;
    }

    public void setVisitorOrigin(String visitorOrigin) {
        this.visitorOrigin = visitorOrigin;
    }

    public String getVisitorUserAgent() {
        return visitorUserAgent;
    }

    public void setVisitorUserAgent(String visitorUserAgent) {
        this.visitorUserAgent = visitorUserAgent;
    }

    public ChatConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ChatConversationStatus status) {
        this.status = status;
    }

    public String getAssignedOperatorUsername() {
        return assignedOperatorUsername;
    }

    public void setAssignedOperatorUsername(String assignedOperatorUsername) {
        this.assignedOperatorUsername = assignedOperatorUsername;
    }

    public Integer getUnreadForOperator() {
        return unreadForOperator;
    }

    public void setUnreadForOperator(Integer unreadForOperator) {
        this.unreadForOperator = unreadForOperator;
    }

    public Integer getUnreadForVisitor() {
        return unreadForVisitor;
    }

    public void setUnreadForVisitor(Integer unreadForVisitor) {
        this.unreadForVisitor = unreadForVisitor;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
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

