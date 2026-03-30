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
        return isPluginEnabled(NOTES_PLUGIN_KEY);
    }

    public boolean isPluginEnabled(String pluginKey) {
        return pluginSettingRepository.findByPluginKey(pluginKey)
                .map(PluginSetting::isEnabled)
                .orElse(true);
    }

    @Transactional
    public boolean setNotesPluginEnabled(boolean enabled) {
        return setPluginEnabled(NOTES_PLUGIN_KEY, enabled);
    }

    @Transactional
    public boolean setPluginEnabled(String pluginKey, boolean enabled) {
        PluginSetting setting = pluginSettingRepository.findByPluginKey(pluginKey)
                .orElseGet(() -> new PluginSetting(pluginKey, true));

        setting.setEnabled(enabled);
        setting.setUpdatedAt(LocalDateTime.now());
        pluginSettingRepository.save(setting);
        return enabled;
    }
}
