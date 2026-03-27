package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Пользователь системы FixByte CRM
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    /** ADMIN или OPERATOR */
    @Column(nullable = false)
    private String role = "OPERATOR";

    private String displayName;

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public AppUser() {}

    public AppUser(String username, String password, String role, String displayName) {
        this.username    = username;
        this.password    = password;
        this.role        = role;
        this.displayName = displayName;
    }

    // Getters / Setters
    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }

    public String getUsername()               { return username; }
    public void setUsername(String u)         { this.username = u; }

    public String getPassword()               { return password; }
    public void setPassword(String p)         { this.password = p; }

    public String getRole()                   { return role; }
    public void setRole(String r)             { this.role = r; }

    public String getDisplayName()            { return displayName; }
    public void setDisplayName(String dn)     { this.displayName = dn; }

    public Boolean getEnabled()               { return enabled; }
    public void setEnabled(Boolean e)         { this.enabled = e; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}

