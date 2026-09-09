package com.rembyte.service;

import com.rembyte.model.PluginSetting;
import com.rembyte.repository.PluginSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginSettingsServiceTest {

    @Mock PluginSettingRepository repository;
    PluginSettingsService service;

    @BeforeEach
    void setUp() {
        service = new PluginSettingsService(repository);
    }

    @Test
    void pluginIsEnabledByDefaultWhenNoRowExists() {
        when(repository.findByPluginKey("notes")).thenReturn(Optional.empty());
        assertThat(service.isNotesPluginEnabled()).isTrue();
    }

    @Test
    void pluginReflectsStoredDisabledFlag() {
        when(repository.findByPluginKey("notes")).thenReturn(Optional.of(new PluginSetting("notes", false)));
        assertThat(service.isNotesPluginEnabled()).isFalse();
    }

    @Test
    void setNotesPluginEnabled_createsRowWhenMissingAndPersistsFlag() {
        when(repository.findByPluginKey("notes")).thenReturn(Optional.empty());

        boolean result = service.setNotesPluginEnabled(false);

        assertThat(result).isFalse();
        ArgumentCaptor<PluginSetting> captor = ArgumentCaptor.forClass(PluginSetting.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPluginKey()).isEqualTo("notes");
        assertThat(captor.getValue().isEnabled()).isFalse();
    }

    @Test
    void setPluginEnabled_updatesExistingRow() {
        PluginSetting existing = new PluginSetting("notes", false);
        when(repository.findByPluginKey("notes")).thenReturn(Optional.of(existing));

        service.setNotesPluginEnabled(true);

        assertThat(existing.isEnabled()).isTrue();
        verify(repository).save(existing);
    }
}
