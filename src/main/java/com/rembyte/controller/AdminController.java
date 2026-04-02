package com.rembyte.controller;

import com.rembyte.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Контроллер страницы настроек администратора.
 * Все маршруты защищены ролью ADMIN.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DatabaseBackupService backupService;

    public AdminController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    // ── Страница настроек ──────────────────────────────────────────────────

    @GetMapping
    public String adminPage() {
        return "admin";
    }

    // ── Скачать резервную копию ────────────────────────────────────────────

    @GetMapping("/backup")
    public void downloadBackup(
            @RequestParam(name = "mode", defaultValue = "full") String mode,
            HttpServletResponse response
    ) throws Exception {
        boolean includeLegacyUploads = !"db".equalsIgnoreCase(mode);
        String filename = "rembyte_backup_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + (includeLegacyUploads ? "_full" : "_db")
                + ".zip";
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        backupService.backupToStream(response.getOutputStream(), includeLegacyUploads);
        response.flushBuffer();
    }

    // ── Восстановить базу данных ───────────────────────────────────────────

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
}

