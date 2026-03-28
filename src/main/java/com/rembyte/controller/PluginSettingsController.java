package com.rembyte.controller;

import com.rembyte.service.PluginSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plugin-settings")
@CrossOrigin(origins = "*")
public class PluginSettingsController {

    private final PluginSettingsService pluginSettingsService;

    public PluginSettingsController(PluginSettingsService pluginSettingsService) {
        this.pluginSettingsService = pluginSettingsService;
    }

    @GetMapping("/notes")
    public ResponseEntity<Map<String, Object>> getNotesPluginSetting() {
        return ResponseEntity.ok(Map.of(
                "pluginKey", PluginSettingsService.NOTES_PLUGIN_KEY,
                "enabled", pluginSettingsService.isNotesPluginEnabled()
        ));
    }

    @PutMapping("/notes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateNotesPluginSetting(@RequestBody Map<String, Object> request) {
        boolean enabled = Boolean.TRUE.equals(request.get("enabled"));
        boolean result = pluginSettingsService.setNotesPluginEnabled(enabled);

        return ResponseEntity.ok(Map.of(
                "pluginKey", PluginSettingsService.NOTES_PLUGIN_KEY,
                "enabled", result
        ));
    }
}

