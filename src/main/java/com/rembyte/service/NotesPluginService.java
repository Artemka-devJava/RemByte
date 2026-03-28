package com.rembyte.service;

import com.rembyte.model.NoteFolder;
import com.rembyte.model.NoteItem;
import com.rembyte.repository.NoteFolderRepository;
import com.rembyte.repository.NoteItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotesPluginService {

    private final NoteFolderRepository noteFolderRepository;
    private final NoteItemRepository noteItemRepository;

    public NotesPluginService(NoteFolderRepository noteFolderRepository, NoteItemRepository noteItemRepository) {
        this.noteFolderRepository = noteFolderRepository;
        this.noteItemRepository = noteItemRepository;
    }

    public List<NoteFolder> getFolders() {
        return noteFolderRepository.findAll();
    }

    @Transactional
    public NoteFolder createFolder(NoteFolder folderData) {
        String name = sanitize(folderData.getName());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Название папки обязательно");
        }

        if (noteFolderRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Папка с таким названием уже существует");
        }

        NoteFolder folder = new NoteFolder();
        folder.setName(name);
        folder.setDescription(sanitize(folderData.getDescription()));
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());
        return noteFolderRepository.save(folder);
    }

    @Transactional
    public NoteFolder updateFolder(Long folderId, NoteFolder folderData) {
        NoteFolder folder = noteFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Папка не найдена"));

        String name = sanitize(folderData.getName());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Название папки обязательно");
        }

        boolean duplicate = noteFolderRepository.existsByNameIgnoreCase(name)
                && !folder.getName().equalsIgnoreCase(name);
        if (duplicate) {
            throw new IllegalArgumentException("Папка с таким названием уже существует");
        }

        folder.setName(name);
        folder.setDescription(sanitize(folderData.getDescription()));
        folder.setUpdatedAt(LocalDateTime.now());
        return noteFolderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Long folderId) {
        if (!noteFolderRepository.existsById(folderId)) {
            throw new RuntimeException("Папка не найдена");
        }
        noteItemRepository.deleteByFolderId(folderId);
        noteFolderRepository.deleteById(folderId);
    }

    public List<NoteItem> getNotes(Long folderId) {
        if (!noteFolderRepository.existsById(folderId)) {
            throw new RuntimeException("Папка не найдена");
        }
        return noteItemRepository.findByFolderIdOrderByUpdatedAtDesc(folderId);
    }

    @Transactional
    public NoteItem createNote(Long folderId, NoteItem noteData) {
        NoteFolder folder = noteFolderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Папка не найдена"));

        NoteItem note = new NoteItem();
        note.setFolder(folder);
        note.setTitle(defaultTitle(noteData.getTitle()));
        note.setContent(sanitizeNoteContent(noteData.getContent()));
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteItemRepository.save(note);
    }

    @Transactional
    public NoteItem updateNote(Long noteId, NoteItem noteData) {
        NoteItem note = noteItemRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена"));

        note.setTitle(defaultTitle(noteData.getTitle()));
        note.setContent(sanitizeNoteContent(noteData.getContent()));
        note.setUpdatedAt(LocalDateTime.now());
        return noteItemRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long noteId) {
        if (!noteItemRepository.existsById(noteId)) {
            throw new RuntimeException("Заметка не найдена");
        }
        noteItemRepository.deleteById(noteId);
    }

    public Optional<NoteItem> getNoteById(Long noteId) {
        return noteItemRepository.findById(noteId);
    }

    public long countNotesInFolder(Long folderId) {
        return noteItemRepository.countByFolderId(folderId);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String sanitizeNoteContent(String value) {
        return value == null ? "" : value;
    }

    private String defaultTitle(String value) {
        String normalized = sanitize(value);
        return normalized.isBlank() ? "Без названия" : normalized;
    }
}

