package com.rembyte.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Защита от подбора пароля: временная блокировка логина после серии
 * неудачных попыток. Форма /login и /api/auth/login (AuthController) идут
 * через один AuthenticationManager, поэтому оба используют один и тот же
 * механизм — слушаем события Spring Security и проверяем блокировку в
 * AppUserService.loadUserByUsername (accountLocked → LockedException).
 * Счётчики хранятся в памяти процесса: для одного инстанса приложения этого
 * достаточно, при рестарте они сбрасываются.
 */
@Service
public class LoginAttemptService {

    private static final class Attempt {
        final AtomicInteger failures = new AtomicInteger(0);
        volatile Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(
            @Value("${fixbyte.security.login.max-attempts:5}") int maxAttempts,
            @Value("${fixbyte.security.login.lock-minutes:15}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) return false;
        Attempt attempt = attempts.get(key(username));
        if (attempt == null || attempt.lockedUntil == null) return false;
        if (Instant.now().isAfter(attempt.lockedUntil)) {
            // Срок блокировки истёк — сбрасываем счётчик, чтобы не блокировать вечно.
            attempts.remove(key(username));
            return false;
        }
        return true;
    }

    @EventListener
    public void onLoginFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication() != null ? event.getAuthentication().getName() : null;
        if (username == null || username.isBlank()) return;

        Attempt attempt = attempts.computeIfAbsent(key(username), k -> new Attempt());
        int count = attempt.failures.incrementAndGet();
        if (count >= maxAttempts) {
            attempt.lockedUntil = Instant.now().plus(lockDuration);
        }
    }

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
