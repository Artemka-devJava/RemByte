package com.rembyte.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Реквизиты компании для шапки чека/квитанции и акта приёмки.
 * Хранится ровно одна строка (id = 1).
 */
@Entity
@Table(name = "company_settings")
public class CompanySettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(length = 200)
    private String name;

    @Column(length = 300)
    private String subtitle;

    @Column(length = 300)
    private String address;

    @Column(length = 120)
    private String phone;

    @Column(length = 160)
    private String email;

    /** Сотрудник по умолчанию (строка «Принял» в чеке). */
    @Column(length = 160)
    private String employee;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
