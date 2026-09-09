package com.rembyte.service;

import com.rembyte.model.NoteFolder;
import com.rembyte.model.NoteItem;
import com.rembyte.repository.NoteFolderRepository;
import com.rembyte.repository.NoteItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotesPluginServiceTest {

    @Mock NoteFolderRepository folderRepository;
    @Mock NoteItemRepository itemRepository;
    NotesPluginService service;

    @BeforeEach
    void setUp() {
        service = new NotesPluginService(folderRepository, itemRepository);
    }

    @Test
    void createFolder_trimsNameAndRejectsBlank() {
        NoteFolder blank = new NoteFolder();
        blank.setName("   ");
        assertThatThrownBy(() -> service.createFolder(blank)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createFolder_rejectsDuplicateNameCaseInsensitively() {
        NoteFolder f = new NoteFolder();
        f.setName("  Клиенты  ");
        when(folderRepository.existsByNameIgnoreCase("Клиенты")).thenReturn(true);

        assertThatThrownBy(() -> service.createFolder(f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    void createFolder_savesTrimmedName() {
        NoteFolder f = new NoteFolder();
        f.setName("  Гарантии  ");
        when(folderRepository.existsByNameIgnoreCase("Гарантии")).thenReturn(false);
        when(folderRepository.save(any(NoteFolder.class))).thenAnswer(i -> i.getArgument(0));

        NoteFolder saved = service.createFolder(f);

        assertThat(saved.getName()).isEqualTo("Гарантии");
    }

    @Test
    void createNote_emptyTitleGetsPlaceholder() {
        NoteFolder folder = new NoteFolder();
        folder.setName("Папка");
        when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
        when(itemRepository.save(any(NoteItem.class))).thenAnswer(i -> i.getArgument(0));

        NoteItem data = new NoteItem();
        data.setTitle("   ");
        data.setContent("тело заметки");

        NoteItem saved = service.createNote(1L, data);

        assertThat(saved.getTitle()).isEqualTo("Без названия");
        assertThat(saved.getContent()).isEqualTo("тело заметки");
        assertThat(saved.getFolder()).isSameAs(folder);
    }

    @Test
    void createNote_missingFolderThrows() {
        when(folderRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createNote(9L, new NoteItem()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteFolder_removesNotesThenFolder() {
        when(folderRepository.existsById(1L)).thenReturn(true);

        service.deleteFolder(1L);

        ArgumentCaptor<Long> id = ArgumentCaptor.forClass(Long.class);
        verify(itemRepository).deleteByFolderId(1L);
        verify(folderRepository).deleteById(id.capture());
        assertThat(id.getValue()).isEqualTo(1L);
    }

    @Test
    void deleteFolder_missingFolderThrows() {
        when(folderRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteFolder(1L)).isInstanceOf(RuntimeException.class);
    }
}
