package com.rembyte.service;

import com.rembyte.model.BackupSchedule;
import com.rembyte.repository.BackupScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupScheduleServiceTest {

    @Mock BackupScheduleRepository repo;
    @Mock BackupStorageService storage;

    BackupScheduleService service;

    @BeforeEach
    void setUp() {
        service = new BackupScheduleService(repo, storage);
        lenient().when(repo.save(any(BackupSchedule.class))).thenAnswer(i -> i.getArgument(0));
    }

    private BackupSchedule row(BackupFrequency freq, LocalDateTime lastRun) {
        BackupSchedule s = new BackupSchedule();
        s.setId(BackupSchedule.SINGLETON_ID);
        s.setFrequency(freq);
        s.setLastRunAt(lastRun);
        return s;
    }

    @Test
    void current_lazilyCreatesAnOffRow() {
        when(repo.findById(BackupSchedule.SINGLETON_ID)).thenReturn(Optional.empty());

        BackupSchedule s = service.current();

        assertThat(s.getFrequency()).isEqualTo(BackupFrequency.OFF);
        verify(repo).save(any(BackupSchedule.class));
    }

    @Test
    void setFrequency_persistsChosenValue() {
        when(repo.findById(BackupSchedule.SINGLETON_ID)).thenReturn(Optional.of(row(BackupFrequency.OFF, null)));

        BackupSchedule s = service.setFrequency(BackupFrequency.QUARTERLY);

        assertThat(s.getFrequency()).isEqualTo(BackupFrequency.QUARTERLY);
    }

    @Test
    void nightlyCheck_doesNothingWhenScheduleIsOff() throws Exception {
        when(repo.findById(BackupSchedule.SINGLETON_ID)).thenReturn(Optional.of(row(BackupFrequency.OFF, null)));

        service.nightlyCheck();

        verify(storage, never()).storeFullBackup();
    }

    @Test
    void nightlyCheck_doesNothingWhenNotYetDue() throws Exception {
        when(repo.findById(BackupSchedule.SINGLETON_ID))
                .thenReturn(Optional.of(row(BackupFrequency.DAILY, LocalDateTime.now())));

        service.nightlyCheck();

        verify(storage, never()).storeFullBackup();
    }

    @Test
    void nightlyCheck_runsBackupAndStampsLastRunWhenDueAndStorageConfigured() throws Exception {
        BackupSchedule s = row(BackupFrequency.DAILY, LocalDateTime.now().minusDays(2));
        when(repo.findById(BackupSchedule.SINGLETON_ID)).thenReturn(Optional.of(s));
        when(storage.isConfigured()).thenReturn(true);
        when(storage.storeFullBackup()).thenReturn(
                new BackupStorageService.StoredBackup("rembyte_backup_20260909_033000_full.zip", 123L, Instant.now()));

        service.nightlyCheck();

        verify(storage).storeFullBackup();
        assertThat(s.getLastRunAt()).isNotNull();
        assertThat(s.getLastStatus()).startsWith("успех");
    }

    @Test
    void nightlyCheck_dueButStorageNotConfigured_skipsAndRecordsReason() throws Exception {
        LocalDateTime originalLastRun = LocalDateTime.now().minusMonths(2);
        BackupSchedule s = row(BackupFrequency.MONTHLY, originalLastRun);
        when(repo.findById(BackupSchedule.SINGLETON_ID)).thenReturn(Optional.of(s));
        when(storage.isConfigured()).thenReturn(false);

        service.nightlyCheck();

        verify(storage, never()).storeFullBackup();
        assertThat(s.getLastRunAt()).isEqualTo(originalLastRun); // не сдвигаем — попробуем снова
        assertThat(s.getLastStatus()).contains("не настроено хранилище");
    }

    @Test
    void nightlyCheck_backupFailure_keepsLastRunUnchangedForRetryAndRecordsError() throws Exception {
        BackupSchedule s = row(BackupFrequency.YEARLY, null);
        when(repo.findById(BackupSchedule.SINGLETON_ID)).thenReturn(Optional.of(s));
        when(storage.isConfigured()).thenReturn(true);
        when(storage.storeFullBackup()).thenThrow(new RuntimeException("шара недоступна"));

        service.nightlyCheck();

        assertThat(s.getLastRunAt()).isNull();
        assertThat(s.getLastStatus()).contains("ошибка").contains("шара недоступна");
    }
}
