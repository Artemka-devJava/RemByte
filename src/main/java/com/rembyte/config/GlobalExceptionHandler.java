package com.rembyte.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.NoSuchElementException;

/**
 * Единая обработка исключений, не пойманных в самих контроллерах. Без этого
 * advice ошибка в сервисе (например, RuntimeException("не найден")) падала в
 * дефолтный Whitelabel/JSON ответ Spring Boot — не по-русски и без единого
 * формата. Здесь: для /api/** — JSON {status, error}, для обычных страниц —
 * templates/error.html на русском с нужным HTTP-статусом.
 *
 * Заказы/клиенты/услуги и т.д. в основном бросают IllegalArgumentException
 * или голый RuntimeException с уже понятным русским сообщением (нет своей
 * иерархии исключений) — поэтому message() просто пробрасывает его клиенту,
 * а не заменяет на общий текст.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ModelAndView handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.NOT_FOUND, message(ex, "Не найдено"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.FORBIDDEN, "Доступ запрещён");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ModelAndView handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.BAD_REQUEST, message(ex, "Некорректный запрос"));
    }

    // Нарушение ограничения БД (уникальность, NOT NULL и т.п.) — например,
    // гонка двух одновременных запросов мимо прикладной проверки дублей.
    // ex.getMessage() тут — сырой текст JDBC/Hibernate (имя constraint-а,
    // иногда кусок SQL) и клиенту не показывается, только в лог.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Нарушение целостности данных на {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respond(request, HttpStatus.CONFLICT, "Операция конфликтует с текущими данными (например, значение уже занято)");
    }

    // Встроенные исключения Spring MVC (ErrorResponseException — например,
    // MissingServletRequestParameterException, HttpMediaTypeNotSupportedException)
    // уже несут свой правильный HTTP-статус — используем его, а не 400/500 по
    // умолчанию. Их ex.getMessage() — техническая английская строка для логов,
    // а не для пользователя, поэтому клиенту отдаём только RU-фолбэк.
    @ExceptionHandler(ErrorResponseException.class)
    public ModelAndView handleErrorResponseException(ErrorResponseException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return respond(request, status, defaultMessageFor(status));
    }

    // NoResourceFoundException (несуществующий URL) — отдельная иерархия:
    // это ServletException (checked), а не RuntimeException, но тоже
    // реализует ErrorResponse со своим статусом (404).
    @ExceptionHandler(ServletException.class)
    public ModelAndView handleServletException(ServletException ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            return respond(request, status, defaultMessageFor(status));
        }
        log.error("Необработанная ServletException на {}", request.getRequestURI(), ex);
        return respond(request, HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
    }

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.warn("Необработанное runtime-исключение на {}: {}", request.getRequestURI(), ex.toString());
        return respond(request, HttpStatus.BAD_REQUEST, message(ex, "Не удалось выполнить операцию"));
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAny(Exception ex, HttpServletRequest request) {
        log.error("Необработанная ошибка на {}", request.getRequestURI(), ex);
        return respond(request, HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
    }

    private ModelAndView respond(HttpServletRequest request, HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView();
        mav.setStatus(status);
        if (request.getRequestURI().startsWith("/api/")) {
            mav.setView(new MappingJackson2JsonView());
            mav.addObject("status", status.value());
            mav.addObject("error", message);
        } else {
            mav.setViewName("error");
            mav.addObject("status", status.value());
            mav.addObject("message", message);
        }
        return mav;
    }

    private String message(Exception ex, String fallback) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? fallback : msg;
    }

    private String defaultMessageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Страница не найдена";
            case METHOD_NOT_ALLOWED -> "Метод не поддерживается";
            case UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE -> "Некорректный формат запроса";
            default -> status.is5xxServerError() ? "Внутренняя ошибка сервера" : "Некорректный запрос";
        };
    }
}
