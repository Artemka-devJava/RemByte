package com.rembyte.controller;

import com.rembyte.model.NoteFolder;
import com.rembyte.model.NoteItem;
import com.rembyte.service.NotesPluginService;
import com.rembyte.service.PluginSettingsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes-plugin")
@CrossOrigin(origins = "*")
public class NotesPluginController {

    private final NotesPluginService notesPluginService;
    private final PluginSettingsService pluginSettingsService;

    public NotesPluginController(NotesPluginService notesPluginService, PluginSettingsService pluginSettingsService) {
        this.notesPluginService = notesPluginService;
        this.pluginSettingsService = pluginSettingsService;
    }

    @GetMapping("/folders")
    public ResponseEntity<List<FolderDto>> getFolders() {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<FolderDto> folders = notesPluginService.getFolders().stream()
                .map(f -> new FolderDto(
                        f.getId(),
                        f.getName(),
                        f.getDescription(),
                        f.getCreatedAt().toString(),
                        f.getUpdatedAt().toString(),
                        notesPluginService.countNotesInFolder(f.getId())
                ))
                .toList();
        return ResponseEntity.ok(folders);
    }

    @PostMapping("/folders")
    public ResponseEntity<?> createFolder(@RequestBody NoteFolder folderData) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            NoteFolder folder = notesPluginService.createFolder(folderData);
            return ResponseEntity.status(HttpStatus.CREATED).body(folder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/folders/{folderId}")
    public ResponseEntity<?> updateFolder(@PathVariable Long folderId, @RequestBody NoteFolder folderData) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            return ResponseEntity.ok(notesPluginService.updateFolder(folderId, folderData));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long folderId) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            notesPluginService.deleteFolder(folderId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @GetMapping("/folders/{folderId}/notes")
    public ResponseEntity<?> getNotes(@PathVariable Long folderId) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            return ResponseEntity.ok(notesPluginService.getNotes(folderId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PostMapping("/folders/{folderId}/notes")
    public ResponseEntity<?> createNote(@PathVariable Long folderId, @RequestBody NoteItem noteData) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(notesPluginService.createNote(folderId, noteData));
        } catch (RuntimeException e) {
            HttpStatus status = "Папка не найдена".equals(e.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(error(e.getMessage()));
        }
    }

    @PutMapping("/notes/{noteId}")
    public ResponseEntity<?> updateNote(@PathVariable Long noteId, @RequestBody NoteItem noteData) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            return ResponseEntity.ok(notesPluginService.updateNote(noteId, noteData));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<?> deleteNote(@PathVariable Long noteId) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Плагин заметок отключен"));
        }
        try {
            notesPluginService.deleteNote(noteId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @GetMapping("/notes/{noteId}/download")
    public ResponseEntity<Object> downloadNote(@PathVariable Long noteId) {
        if (!pluginSettingsService.isNotesPluginEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body((Object) error("Плагин заметок отключен"));
        }
        return notesPluginService.getNoteById(noteId)
                .map(note -> {
                    String fileName = toSafeFileName(note.getTitle()) + ".txt";
                    String body = "# " + note.getTitle() + "\n\n" + note.getContent();
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeRFC5987(fileName))
                            .contentType(MediaType.TEXT_PLAIN)
                            .body((Object) body);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body((Object) error("Заметка не найдена")));
    }

    private String toSafeFileName(String title) {
        String normalized = (title == null || title.isBlank()) ? "note" : title.trim();
        return normalized.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    private String encodeRFC5987(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_') {
                sb.append((char) c);
            } else {
                sb.append('%');
                sb.append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }

    private Map<String, String> error(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private record FolderDto(
            Long id,
            String name,
            String description,
            String createdAt,
            String updatedAt,
            long notesCount
    ) {}
}

