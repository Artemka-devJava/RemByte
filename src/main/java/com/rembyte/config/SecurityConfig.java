package com.rembyte.config;

import com.rembyte.service.AppUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(appUserService)
            .authorizeHttpRequests(auth -> auth
                // Публичные ресурсы
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                // H2 консоль
                .requestMatchers("/h2-console/**").permitAll()
                // Админские страницы
                .requestMatchers("/users", "/api/users/**", "/admin", "/admin/**").hasRole("ADMIN")
                // Всё остальное — только авторизованным
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Отключить CSRF для API и H2 консоли
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/h2-console/**", "/admin/restore")
            )
            // Разрешить iframe для H2 консоли
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
