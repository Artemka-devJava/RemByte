package com.rembyte.controller;

import com.rembyte.model.CompanySettings;
import com.rembyte.service.CompanySettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реквизиты компании для шапки чека/квитанции и акта приёмки.
 * Читать может любой авторизованный пользователь; менять — только ADMIN.
 */
@RestController
@RequestMapping("/api/settings/company")
@CrossOrigin(origins = "*")
public class CompanySettingsController {

    private final CompanySettingsService service;

    public CompanySettingsController(CompanySettingsService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> get() {
        return toMap(service.current());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> update(@RequestBody CompanySettings body) {
        return toMap(service.update(body));
    }

    private static Map<String, Object> toMap(CompanySettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", s.getName());
        m.put("subtitle", s.getSubtitle());
        m.put("address", s.getAddress());
        m.put("phone", s.getPhone());
        m.put("email", s.getEmail());
        m.put("employee", s.getEmployee());
        return m;
    }
}
