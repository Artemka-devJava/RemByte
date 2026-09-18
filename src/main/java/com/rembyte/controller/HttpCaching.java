package com.rembyte.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

/**
 * HTTP-кэширование для отдачи бинарных вложений (фото/файлы), хранящихся в
 * БД как LONGBLOB. Раньше эти байты гонялись на каждый показ страницы
 * заново — теперь браузер один раз получает файл и переиспользует его,
 * пока не увидит другой id/имя файла.
 * <p>
 * Содержимое таких вложений неизменяемо: заменить файл нельзя, только
 * добавить новый или удалить — поэтому подходит простой сильный ETag на
 * основе id/имени файла плюс {@code immutable}, без сверки даты изменения.
 */
final class HttpCaching {

    private static final String CACHE_CONTROL_VALUE = "private, max-age=31536000, immutable";

    private HttpCaching() {
    }

    private static String quoted(String eTagValue) {
        return "\"" + eTagValue + "\"";
    }

    /** Если у клиента уже есть этот байт-код (по {@code If-None-Match}) — готовый ответ {@code 304}. */
    static Optional<ResponseEntity<byte[]>> notModified(HttpServletRequest request, String eTagValue) {
        String eTag = quoted(eTagValue);
        String ifNoneMatch = request == null ? null : request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (!eTag.equals(ifNoneMatch)) {
            return Optional.empty();
        }
        return Optional.of(ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(eTag)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .build());
    }

    /** Заголовки кэша для ответа {@code 200} с этим ETag'ом — добавить билдеру перед {@code .body(...)}. */
    static ResponseEntity.BodyBuilder withCacheHeaders(ResponseEntity.BodyBuilder builder, String eTagValue) {
        return builder.eTag(quoted(eTagValue)).header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE);
    }

    /** Короткий путь для эндпоинтов без дополнительных заголовков (Content-Disposition и т.п.). */
    static ResponseEntity<byte[]> respond(HttpServletRequest request, String eTagValue,
                                           MediaType mediaType, byte[] content) {
        return notModified(request, eTagValue)
                .orElseGet(() -> withCacheHeaders(ResponseEntity.ok(), eTagValue)
                        .contentType(mediaType)
                        .body(content));
    }
}
