package wiki.kana.serviceUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import wiki.kana.entity.User;
import wiki.kana.repository.UserRepository;
import wiki.kana.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthenticateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void authenticateWhenUserIsActiveNullShouldThrowIllegalStateException() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("$2a$10$hash")
                .isActive(null)
                .role(User.UserRole.ADMIN)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.authenticate("admin", "admin123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deactivated");

        verify(userRepository).findByUsername("admin");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void authenticateWhenUserIsActiveAndPasswordMatchesShouldReturnUser() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("$2a$10$hash")
                .isActive(true)
                .role(User.UserRole.ADMIN)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.matches("admin123", user.getPassword())).thenReturn(true);

        User authenticated = userService.authenticate("admin", "admin123");
        assertThat(authenticated.getId()).isEqualTo(1L);
        assertThat(authenticated.getUsername()).isEqualTo("admin");

        verify(userRepository).findByUsername("admin");
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }
}
