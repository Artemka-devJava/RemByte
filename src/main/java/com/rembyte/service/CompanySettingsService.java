package com.rembyte.service;

import com.rembyte.model.CompanySettings;
import com.rembyte.repository.CompanySettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Реквизиты компании (шапка чека и акта приёмки). Одна строка в БД;
 * при первом обращении заполняется из {@code fixbyte.company.*}.
 */
@Service
public class CompanySettingsService {

    private final CompanySettingsRepository repo;

    @Value("${fixbyte.company.name:FixByte}")
    private String defName;
    @Value("${fixbyte.company.subtitle:}")
    private String defSubtitle;
    @Value("${fixbyte.company.address:}")
    private String defAddress;
    @Value("${fixbyte.company.phone:}")
    private String defPhone;
    @Value("${fixbyte.company.email:}")
    private String defEmail;

    private static final String DEFAULT_SUBTITLE = "Сервисный центр · ремонт техники";

    public CompanySettingsService(CompanySettingsRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CompanySettings current() {
        return repo.findById(CompanySettings.SINGLETON_ID).orElseGet(() -> {
            CompanySettings s = new CompanySettings();
            s.setId(CompanySettings.SINGLETON_ID);
            s.setName(blankToNull(defName) == null ? "FixByte" : defName.trim());
            s.setSubtitle(blankToNull(defSubtitle) == null ? DEFAULT_SUBTITLE : defSubtitle.trim());
            s.setAddress(blankToNull(defAddress));
            s.setPhone(blankToNull(defPhone));
            s.setEmail(blankToNull(defEmail));
            s.setUpdatedAt(LocalDateTime.now());
            return repo.save(s);
        });
    }

    @Transactional
    public CompanySettings update(CompanySettings data) {
        CompanySettings s = current();
        s.setName(orDefault(data.getName(), "FixByte"));
        s.setSubtitle(trimOrNull(data.getSubtitle()));
        s.setAddress(trimOrNull(data.getAddress()));
        s.setPhone(trimOrNull(data.getPhone()));
        s.setEmail(trimOrNull(data.getEmail()));
        s.setEmployee(trimOrNull(data.getEmployee()));
        s.setUpdatedAt(LocalDateTime.now());
        return repo.save(s);
    }

    private static String orDefault(String v, String fallback) {
        String t = trimOrNull(v);
        return t == null ? fallback : t;
    }

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
