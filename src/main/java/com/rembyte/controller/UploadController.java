package com.rembyte.controller;

import com.rembyte.service.FileStorageService;
import com.rembyte.service.KanbanService;
import jakarta.servlet.http.HttpServletRequest;
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
                                                     @PathVariable String storedName,
                                                     HttpServletRequest request) {
        try {
            FileStorageService.StoredAttachment attachment = fileStorageService.loadOrderAttachment(orderId, storedName);

            MediaType mediaType = resolveMediaType(attachment.contentType());
            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(attachment.originalName(), StandardCharsets.UTF_8)
                    .build();

            // storedName содержит timestamp+UUID — сам по себе уже уникальный
            // и неизменяемый идентификатор содержимого, годится как ETag.
            return HttpCaching.notModified(request, storedName)
                    .orElseGet(() -> HttpCaching.withCacheHeaders(ResponseEntity.ok(), storedName)
                            .contentType(mediaType)
                            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                            .body(attachment.content()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/uploads/kanban/{cardId}/{storedName}")
    public ResponseEntity<byte[]> getKanbanAttachment(@PathVariable Long cardId,
                                                      @PathVariable String storedName,
                                                      Authentication authentication,
                                                      HttpServletRequest request) {
        try {
            KanbanService.StoredAttachment attachment = kanbanService.loadCardAttachment(authentication.getName(), cardId, storedName);

            MediaType mediaType = resolveMediaType(attachment.contentType());
            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(attachment.originalName(), StandardCharsets.UTF_8)
                    .build();

            return HttpCaching.notModified(request, storedName)
                    .orElseGet(() -> HttpCaching.withCacheHeaders(ResponseEntity.ok(), storedName)
                            .contentType(mediaType)
                            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                            .body(attachment.content()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
