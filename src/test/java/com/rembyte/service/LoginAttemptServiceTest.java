package com.rembyte.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private void fail(LoginAttemptService service, String username) {
        service.onLoginFailure(new AuthenticationFailureBadCredentialsEvent(
                new UsernamePasswordAuthenticationToken(username, "wrong"),
                new BadCredentialsException("bad creds")));
    }

    @Test
    void notLockedBeforeThreshold() {
        LoginAttemptService service = new LoginAttemptService(5, 15);
        for (int i = 0; i < 4; i++) fail(service, "admin");
        assertThat(service.isLocked("admin")).isFalse();
    }

    @Test
    void locksAfterMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService(5, 15);
        for (int i = 0; i < 5; i++) fail(service, "admin");
        assertThat(service.isLocked("admin")).isTrue();
    }

    @Test
    void lockIsCaseInsensitiveAndPerUsername() {
        LoginAttemptService service = new LoginAttemptService(2, 15);
        fail(service, "Admin");
        fail(service, "ADMIN");
        assertThat(service.isLocked("admin")).isTrue();
        assertThat(service.isLocked("operator")).isFalse();
    }

    @Test
    void successfulLoginResetsCounter() {
        LoginAttemptService service = new LoginAttemptService(3, 15);
        fail(service, "admin");
        fail(service, "admin");

        service.onLoginSuccess(new AuthenticationSuccessEvent(
                new UsernamePasswordAuthenticationToken("admin", "secret")));

        fail(service, "admin");
        assertThat(service.isLocked("admin")).isFalse();
    }

    @Test
    void lockExpiresAfterConfiguredDuration() {
        // Отрицательная длительность = блокировка уже "истекла" в момент установки —
        // детерминированный способ проверить авто-разблокировку без sleep().
        LoginAttemptService service = new LoginAttemptService(1, -1);
        fail(service, "admin");
        assertThat(service.isLocked("admin")).isFalse();
    }
}
