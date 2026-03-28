package com.rembyte.controller;

import com.rembyte.service.FileStorageService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
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
}
