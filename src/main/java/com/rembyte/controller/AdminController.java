package com.rembyte.controller;

import com.rembyte.model.BackupSchedule;
import com.rembyte.service.BackupFrequency;
import com.rembyte.service.BackupScheduleService;
import com.rembyte.service.BackupStorageService;
import com.rembyte.service.BackupStorageService.StoredBackup;
import com.rembyte.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Контроллер страницы настроек администратора.
 * Все маршруты защищены ролью ADMIN.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DatabaseBackupService backupService;
    private final BackupStorageService backupStorage;
    private final BackupScheduleService backupSchedule;

    public AdminController(DatabaseBackupService backupService,
                          BackupStorageService backupStorage,
                          BackupScheduleService backupSchedule) {
        this.backupService = backupService;
        this.backupStorage = backupStorage;
        this.backupSchedule = backupSchedule;
    }

    // ── Страница настроек ──────────────────────────────────────────────────

    @GetMapping
    public String adminPage() {
        return "admin";
    }

    // ── Автоматический полный бэкап (расписание) ──────────────────────────

    @GetMapping("/backup/schedule")
    @ResponseBody
    public Map<String, Object> backupScheduleInfo() {
        return scheduleInfo(backupSchedule.current());
    }

    @PutMapping("/backup/schedule")
    @ResponseBody
    public ResponseEntity<?> setBackupSchedule(@RequestBody Map<String, String> body) {
        String raw = body == null ? null : body.get("frequency");
        BackupFrequency freq;
        try {
            freq = BackupFrequency.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неизвестная периодичность"));
        }
        return ResponseEntity.ok(scheduleInfo(backupSchedule.setFrequency(freq)));
    }

    private static Map<String, Object> scheduleInfo(BackupSchedule s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("frequency", s.getFrequency().name());
        m.put("frequencyLabel", s.getFrequency().label());
        m.put("lastRunAt", s.getLastRunAt() == null ? null : s.getLastRunAt().toString());
        m.put("lastStatus", s.getLastStatus());
        return m;
    }

    // ── Хранилище полных бэкапов (каталог / Samba-моунт) ──────────────────

    @GetMapping("/backup/store")
    @ResponseBody
    public Map<String, Object> storeInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configured", backupStorage.isConfigured());
        m.put("target", backupStorage.targetDescription());
        m.put("baseDir", backupStorage.baseDir());
        m.put("dirOverride", backupStorage.directoryOverride());
        m.put("keep", backupStorage.keepCount());
        m.put("backups", backupStorage.list().stream().map(AdminController::toMap).toList());
        return m;
    }

    @PutMapping("/backup/dir")
    @ResponseBody
    public ResponseEntity<?> setBackupDir(@RequestBody Map<String, String> body) {
        try {
            backupStorage.setDirectory(body == null ? null : body.get("dir"));
            return ResponseEntity.ok(storeInfo());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/backup/store")
    @ResponseBody
    public ResponseEntity<?> storeFullBackup() {
        if (!backupStorage.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Хранилище не настроено. Выберите путь в панели или задайте FIXBYTE_BACKUP_DIR."));
        }
        try {
            StoredBackup b = backupStorage.storeFullBackup();
            return ResponseEntity.ok(toMap(b));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error",
                    "Не удалось сохранить бэкап: " + e.getMessage()));
        }
    }

    @GetMapping("/backup/store/{name:.+}")
    public void downloadStored(@PathVariable String name, HttpServletResponse response) throws Exception {
        long size;
        try {
            size = backupStorage.size(name);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
        if (size >= 0) {
            response.setContentLengthLong(size);
        }
        try (InputStream in = backupStorage.open(name); OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
        }
        response.flushBuffer();
    }

    @PostMapping("/backup/store/{name:.+}/restore")
    @ResponseBody
    public ResponseEntity<String> restoreStored(@PathVariable String name) {
        try (InputStream in = backupStorage.open(name)) {
            backupService.restoreFromStream(in);
            return ResponseEntity.ok("✅ Восстановлено из " + name);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Ошибка восстановления: " + e.getMessage());
        }
    }

    @DeleteMapping("/backup/store/{name:.+}")
    @ResponseBody
    public ResponseEntity<String> deleteStored(@PathVariable String name) {
        try {
            backupStorage.delete(name);
            return ResponseEntity.ok("Удалено: " + name);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Не удалось удалить: " + e.getMessage());
        }
    }

    private static Map<String, Object> toMap(StoredBackup b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", b.name());
        m.put("size", b.size());
        m.put("modified", b.modified().toString());
        return m;
    }
}
