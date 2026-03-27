package com.rembyte.service;

import com.rembyte.model.AppUser;
import com.rembyte.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис управления пользователями + реализация UserDetailsService для Spring Security
 */
@Service
@Transactional
public class AppUserService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ===== Spring Security =====

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException("Пользователь заблокирован: " + username);
        }

        return User.builder()
            .username(user.getUsername())
            .password("{noop}" + user.getPassword())
            .roles(user.getRole())   // ADMIN → ROLE_ADMIN, OPERATOR → ROLE_OPERATOR
            .build();
    }

    // ===== CRUD =====

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public AppUser getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
    }

    public AppUser createUser(AppUser user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Логин уже занят: " + user.getUsername());
        }
        return userRepository.save(user);
    }

    public AppUser updateUser(Long id, AppUser data) {
        return userRepository.findById(id).map(user -> {
            user.setDisplayName(data.getDisplayName());
            user.setRole(data.getRole());
            user.setEnabled(data.getEnabled());
            // Сменить пароль только если передан не пустой
            if (data.getPassword() != null && !data.getPassword().isBlank()) {
                user.setPassword(data.getPassword());
            }
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /** Инициализация пользователей по умолчанию */
    public void initDefaultUsers(String adminUsername, String adminPassword,
                                  String operatorUsername, String operatorPassword) {
        if (userRepository.count() == 0) {
            userRepository.save(new AppUser(adminUsername, adminPassword, "ADMIN", "Администратор"));
            userRepository.save(new AppUser(operatorUsername, operatorPassword, "OPERATOR", "Оператор"));
        }
    }
}

