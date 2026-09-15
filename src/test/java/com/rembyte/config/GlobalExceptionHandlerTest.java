package com.rembyte.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock HttpServletRequest apiRequest;
    @Mock HttpServletRequest pageRequest;

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void wireMocks() {
        lenient().when(apiRequest.getRequestURI()).thenReturn("/api/orders/999");
        lenient().when(pageRequest.getRequestURI()).thenReturn("/orders/unknown-path");
    }

    @Test
    void notFound_apiRequest_returnsJsonWith404() {
        ModelAndView mav = handler.handleNotFound(new NoSuchElementException("Заказ не найден"), apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mav.getView()).isInstanceOf(MappingJackson2JsonView.class);
        assertThat(mav.getModel()).containsEntry("error", "Заказ не найден").containsEntry("status", 404);
    }

    @Test
    void notFound_pageRequest_rendersErrorTemplate() {
        ModelAndView mav = handler.handleNotFound(new NoSuchElementException("Заказ не найден"), pageRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getModel()).containsEntry("message", "Заказ не найден");
    }

    @Test
    void accessDenied_mapsTo403WithFixedMessage_regardlessOfInternalDetails() {
        ModelAndView mav = handler.handleAccessDenied(new AccessDeniedException("internal ACL detail"), apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mav.getModel()).containsEntry("error", "Доступ запрещён");
    }

    @Test
    void illegalArgument_mapsTo400AndPassesThroughBusinessMessage() {
        ModelAndView mav = handler.handleBadRequest(new IllegalArgumentException("Логин уже занят: admin"), apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mav.getModel()).containsEntry("error", "Логин уже занят: admin");
    }

    @Test
    void dataIntegrityViolation_mapsTo409_andNeverLeaksRawJdbcMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_CLIENTS_PHONE]");

        ModelAndView mav = handler.handleDataIntegrityViolation(ex, apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        String error = String.valueOf(mav.getModel().get("error"));
        assertThat(error).doesNotContain("SQL").doesNotContain("UK_CLIENTS_PHONE").doesNotContain("constraint");
    }

    @Test
    void errorResponseException_usesItsOwnStatusCode() {
        ErrorResponseException ex = new ErrorResponseException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        ModelAndView mav = handler.handleErrorResponseException(ex, apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void noResourceFoundException_isServletExceptionButStillMapsTo404() {
        NoResourceFoundException ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "no-such-page");

        ModelAndView mav = handler.handleServletException(ex, pageRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mav.getViewName()).isEqualTo("error");
    }

    @Test
    void genericRuntimeException_fallsBackTo400WithItsMessage() {
        ModelAndView mav = handler.handleRuntime(new RuntimeException("Клиент не найден"), apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mav.getModel()).containsEntry("error", "Клиент не найден");
    }

    @Test
    void unexpectedException_mapsTo500WithGenericMessage_notInternalDetails() {
        ModelAndView mav = handler.handleAny(new NullPointerException("null field at line 42"), apiRequest);

        assertThat(mav.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mav.getModel()).containsEntry("error", "Внутренняя ошибка сервера");
    }
}
