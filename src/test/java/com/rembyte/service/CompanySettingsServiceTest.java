package com.rembyte.service;

import com.rembyte.model.CompanySettings;
import com.rembyte.repository.CompanySettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanySettingsServiceTest {

    @Mock CompanySettingsRepository repo;
    CompanySettingsService service;

    @BeforeEach
    void setUp() {
        service = new CompanySettingsService(repo);
        ReflectionTestUtils.setField(service, "defName", "FixByte");
        ReflectionTestUtils.setField(service, "defSubtitle", "");
        ReflectionTestUtils.setField(service, "defAddress", "");
        ReflectionTestUtils.setField(service, "defPhone", "");
        ReflectionTestUtils.setField(service, "defEmail", "");
        lenient().when(repo.save(any(CompanySettings.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void current_lazilyCreatesRowSeededFromDefaults() {
        when(repo.findById(CompanySettings.SINGLETON_ID)).thenReturn(Optional.empty());

        CompanySettings s = service.current();

        assertThat(s.getId()).isEqualTo(CompanySettings.SINGLETON_ID);
        assertThat(s.getName()).isEqualTo("FixByte");
        assertThat(s.getSubtitle()).isEqualTo("Сервисный центр · ремонт техники");
    }

    @Test
    void update_trimsAndBlanksToNullKeepsNameFallback() {
        when(repo.findById(CompanySettings.SINGLETON_ID)).thenReturn(Optional.of(new CompanySettings()));

        CompanySettings data = new CompanySettings();
        data.setName("  Ромашка  ");
        data.setAddress("   ");
        data.setPhone(" +7 999 111-22-33 ");
        data.setEmail("");

        CompanySettings s = service.update(data);

        assertThat(s.getName()).isEqualTo("Ромашка");
        assertThat(s.getAddress()).isNull();
        assertThat(s.getPhone()).isEqualTo("+7 999 111-22-33");
        assertThat(s.getEmail()).isNull();
    }

    @Test
    void update_emptyNameFallsBackToFixByte() {
        when(repo.findById(CompanySettings.SINGLETON_ID)).thenReturn(Optional.of(new CompanySettings()));
        CompanySettings data = new CompanySettings();
        data.setName("   ");

        assertThat(service.update(data).getName()).isEqualTo("FixByte");
    }
}
