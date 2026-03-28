package com.rembyte.repository;

import com.rembyte.model.PluginSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PluginSettingRepository extends JpaRepository<PluginSetting, Long> {
    Optional<PluginSetting> findByPluginKey(String pluginKey);
}

