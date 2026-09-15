package com.rembyte.config;

import com.rembyte.service.AppUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

/**
 * Конфигурация безопасности FixByte CRM
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // включает @PreAuthorize в контроллерах
public class SecurityConfig {

    private final AppUserService appUserService;

    public SecurityConfig(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * Отдельная цепочка только для встраиваемого iframe-виджета чата
     * (/widget/chat/frame): это единственная страница, которой реально нужен
     * X-Frame-Options выключенным, чтобы её можно было встроить на сайт
     * клиента. Всё остальное приложение (включая /login и админку) больше не
     * теряет защиту от clickjacking из-за одной этой страницы.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain widgetFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/widget/chat/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(appUserService)
            .authorizeHttpRequests(auth -> auth
                // Публичные ресурсы
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/public/chat/**").permitAll()
                // Точка входа для мобильного/нативных клиентов
                .requestMatchers("/api/auth/**").permitAll()
                // H2 консоль
                .requestMatchers("/h2-console/**").permitAll()
                // Docker/мониторинг healthcheck — без авторизации, остальной actuator только ADMIN
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Админские страницы
                .requestMatchers("/users", "/api/users/**", "/admin", "/admin/**").hasRole("ADMIN")
                // Всё остальное — только авторизованным
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                // Отдельный редирект для блокировки по брутфорсу (LoginAttemptService),
                // чтобы на /login показать понятное сообщение, а не "неверный пароль".
                .failureHandler((request, response, exception) -> {
                    String redirect = exception instanceof LockedException ? "/login?locked" : "/login?error";
                    response.sendRedirect(request.getContextPath() + redirect);
                })
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            // Неавторизованный запрос к /api/** — это 401 (без редиректа на HTML
            // страницу входа). Иначе мобильный/нативный клиент по умолчанию идёт
            // за 302 → /login → 200 (HTML) и принимает это за успешный ответ:
            // например, «фото загружено», хотя на сервер ничего не попало.
            // Обычные (не /api) страницы по-прежнему редиректят на форму входа.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(apiAwareEntryPoint()))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Отключить CSRF для API и H2 консоли
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/public/chat/**", "/h2-console/**",
                        "/admin/backup/**")
            )
            // X-Frame-Options: SAMEORIGIN — блокирует встраивание в чужие сайты
            // (clickjacking), но не мешает H2-консоли (её внутренние фреймы —
            // тот же origin). Кросс-доменный iframe нужен только виджету чата,
            // у него отдельная цепочка (widgetFilterChain) выше.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Для путей /api/** отдаём чистый 401, для остальных — редирект на /login.
     */
    private AuthenticationEntryPoint apiAwareEntryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> mappings = new LinkedHashMap<>();
        mappings.put(new AntPathRequestMatcher("/api/**"), new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(mappings);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return entryPoint;
    }
}
