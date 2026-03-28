package com.rembyte.service;

import com.rembyte.model.PluginSetting;
import com.rembyte.repository.PluginSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PluginSettingsService {

    public static final String NOTES_PLUGIN_KEY = "notes";

    private final PluginSettingRepository pluginSettingRepository;

    public PluginSettingsService(PluginSettingRepository pluginSettingRepository) {
        this.pluginSettingRepository = pluginSettingRepository;
    }

    public boolean isNotesPluginEnabled() {
        return pluginSettingRepository.findByPluginKey(NOTES_PLUGIN_KEY)
                .map(PluginSetting::isEnabled)
                .orElse(true);
    }

    @Transactional
    public boolean setNotesPluginEnabled(boolean enabled) {
        PluginSetting setting = pluginSettingRepository.findByPluginKey(NOTES_PLUGIN_KEY)
                .orElseGet(() -> new PluginSetting(NOTES_PLUGIN_KEY, true));

        setting.setEnabled(enabled);
        setting.setUpdatedAt(LocalDateTime.now());
        pluginSettingRepository.save(setting);
        return enabled;
    }
}

