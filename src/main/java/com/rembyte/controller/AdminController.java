package com.rembyte.controller;

import com.rembyte.service.BackupLevel;
import com.rembyte.service.BackupStorageService;
import com.rembyte.service.BackupStorageService.StoredBackup;
import com.rembyte.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public AdminController(DatabaseBackupService backupService, BackupStorageService backupStorage) {
        this.backupService = backupService;
        this.backupStorage = backupStorage;
    }

    // ── Страница настроек ──────────────────────────────────────────────────

    @GetMapping
    public String adminPage() {
        return "admin";
    }

    // ── Скачать резервную копию (level=light|full; mode= для совместимости) ─

    @GetMapping("/backup")
    public void downloadBackup(
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "mode", required = false) String mode,
            HttpServletResponse response
    ) throws Exception {
        BackupLevel backupLevel = BackupLevel.parse(level != null ? level : mode);
        String filename = "rembyte_backup_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + backupLevel.fileSuffix() + ".zip";
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        backupService.backupToStream(response.getOutputStream(), backupLevel);
        response.flushBuffer();
    }

    // ── Восстановить из загруженного файла ────────────────────────────────

    @PostMapping("/restore")
    @ResponseBody
    public ResponseEntity<String> restoreBackup(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не выбран");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".sql") && !name.endsWith(".zip")) {
            return ResponseEntity.badRequest().body("Допустимы только файлы .zip или .sql");
        }
        try {
            backupService.restoreFromStream(file.getInputStream());
            return ResponseEntity.ok("✅ Резервная копия успешно восстановлена");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("❌ Ошибка восстановления: " + e.getMessage());
        }
    }

    // ── Хранилище полных бэкапов (каталог / Samba-моунт) ──────────────────

    @GetMapping("/backup/store")
    @ResponseBody
    public Map<String, Object> storeInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configured", backupStorage.isConfigured());
        m.put("target", backupStorage.targetDescription());
        m.put("keep", backupStorage.keepCount());
        m.put("backups", backupStorage.list().stream().map(AdminController::toMap).toList());
        return m;
    }

    @PostMapping("/backup/store")
    @ResponseBody
    public ResponseEntity<?> storeFullBackup() {
        if (!backupStorage.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Хранилище не настроено. Задайте FIXBYTE_BACKUP_DIR (можно смонтированную шару Samba)."));
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
