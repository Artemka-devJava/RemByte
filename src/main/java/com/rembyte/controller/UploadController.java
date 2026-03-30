package com.rembyte.controller;

import com.rembyte.service.FileStorageService;
import com.rembyte.service.KanbanService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class UploadController {

    private final FileStorageService fileStorageService;
    private final KanbanService kanbanService;

    public UploadController(FileStorageService fileStorageService,
                            KanbanService kanbanService) {
        this.fileStorageService = fileStorageService;
        this.kanbanService = kanbanService;
    }

    @GetMapping("/uploads/orders/{orderId}/{storedName}")
    public ResponseEntity<byte[]> getOrderAttachment(@PathVariable Long orderId,
                                                     @PathVariable String storedName) {
        try {
            FileStorageService.StoredAttachment attachment = fileStorageService.loadOrderAttachment(orderId, storedName);

            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(attachment.contentType());
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(attachment.originalName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(attachment.content());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/uploads/kanban/{cardId}/{storedName}")
    public ResponseEntity<byte[]> getKanbanAttachment(@PathVariable Long cardId,
                                                      @PathVariable String storedName,
                                                      Authentication authentication) {
        try {
            KanbanService.StoredAttachment attachment = kanbanService.loadCardAttachment(authentication.getName(), cardId, storedName);

            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(attachment.contentType());
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(attachment.originalName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(attachment.content());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
