package com.rembyte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/**
 * Категория услуг ремонта
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "service_categories")
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String icon = "🔧";

    public ServiceCategory() {}

    public ServiceCategory(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public Long getId()              { return id; }
    public void setId(Long id)       { this.id = id; }

    public String getName()          { return name; }
    public void setName(String n)    { this.name = n; }

    public String getIcon()          { return icon; }
    public void setIcon(String i)    { this.icon = i; }
}

