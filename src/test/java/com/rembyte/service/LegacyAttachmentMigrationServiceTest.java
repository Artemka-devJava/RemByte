package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.OrderAttachment;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyAttachmentMigrationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void migrateOnStartup_shouldBeIdempotent() throws Exception {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderAttachmentRepository attachmentRepository = mock(OrderAttachmentRepository.class);

        Order order = new Order();
        order.setId(1L);

        Path uploadsRoot = tempDir.resolve("uploads").resolve("orders").resolve("1");
        Files.createDirectories(uploadsRoot);
        Files.writeString(uploadsRoot.resolve("test-note.txt"), "legacy file");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> importedNames = new HashSet<>();
        when(attachmentRepository.existsByOrderIdAndStoredName(anyLong(), anyString()))
                .thenAnswer(invocation -> importedNames.contains(invocation.getArgument(1)));
        when(attachmentRepository.save(any(OrderAttachment.class))).thenAnswer(invocation -> {
            OrderAttachment saved = invocation.getArgument(0);
            importedNames.add(saved.getStoredName());
            return saved;
        });

        LegacyAttachmentMigrationService migrationService =
                new LegacyAttachmentMigrationService(orderRepository, attachmentRepository);

        ReflectionTestUtils.setField(migrationService, "uploadDir", tempDir.resolve("uploads").toString());
        ReflectionTestUtils.setField(migrationService, "migrationEnabled", true);
        ReflectionTestUtils.setField(migrationService, "deleteLegacyAfterMigration", false);

        migrationService.migrateOnStartup();
        migrationService.migrateOnStartup();

        verify(attachmentRepository, times(1)).save(any(OrderAttachment.class));
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }
}

