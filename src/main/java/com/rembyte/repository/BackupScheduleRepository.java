package com.rembyte.repository;

import com.rembyte.model.BackupSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {
}
