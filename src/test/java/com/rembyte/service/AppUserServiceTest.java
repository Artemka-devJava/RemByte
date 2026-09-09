package com.rembyte.service;

import com.rembyte.model.AppUser;
import com.rembyte.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock AppUserRepository userRepository;
    AppUserService service;

    @BeforeEach
    void setUp() {
        service = new AppUserService(userRepository);
    }

    @Test
    void loadUserByUsername_mapsRoleAndUsesNoopPasswordEncoding() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", "secret", "ADMIN", "Админ")));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getPassword()).isEqualTo("{noop}secret");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_unknownUserThrows() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_disabledUserThrows() {
        AppUser u = new AppUser("bob", "pw", "OPERATOR", "Боб");
        u.setEnabled(false);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.loadUserByUsername("bob"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("заблокирован");
    }

    @Test
    void createUser_rejectsTakenUsername() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        assertThatThrownBy(() -> service.createUser(new AppUser("admin", "x", "ADMIN", "A")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("занят");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_changesPasswordOnlyWhenNonBlankProvided() {
        AppUser stored = new AppUser("op", "oldpass", "OPERATOR", "Оп");
        when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser blankPw = new AppUser("op", "  ", "ADMIN", "Оп 2");
        service.updateUser(1L, blankPw);
        assertThat(stored.getPassword()).isEqualTo("oldpass");
        assertThat(stored.getRole()).isEqualTo("ADMIN");

        AppUser newPw = new AppUser("op", "brandnew", "ADMIN", "Оп 2");
        service.updateUser(1L, newPw);
        assertThat(stored.getPassword()).isEqualTo("brandnew");
    }

    @Test
    void initDefaultUsers_seedsOnlyWhenTableIsEmpty() {
        when(userRepository.count()).thenReturn(0L);
        service.initDefaultUsers("admin", "a", "operator", "o");
        verify(userRepository, org.mockito.Mockito.times(2)).save(any(AppUser.class));
    }

    @Test
    void initDefaultUsers_noOpWhenUsersExist() {
        when(userRepository.count()).thenReturn(5L);
        service.initDefaultUsers("admin", "a", "operator", "o");
        verify(userRepository, never()).save(any());
    }
}
