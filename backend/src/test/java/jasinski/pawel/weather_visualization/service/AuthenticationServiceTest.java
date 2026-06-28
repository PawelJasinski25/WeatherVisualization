package jasinski.pawel.weather_visualization.service;

import jasinski.pawel.weather_visualization.dto.AuthRequest;
import jasinski.pawel.weather_visualization.dto.AuthResponse;
import jasinski.pawel.weather_visualization.entity.User;
import jasinski.pawel.weather_visualization.repository.UserRepository;
import jasinski.pawel.weather_visualization.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private AuthRequest authRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {

        authRequest = new AuthRequest();
        authRequest.setEmail("email@example.com");
        authRequest.setPassword("example password");

        mockUser = new User();
        mockUser.setEmail("email@example.com");
        mockUser.setPassword("encoded password");
    }

    @Test
    void register_shouldSaveUser_whenEmailIsUnique() {
        when(userRepository.existsByEmail(authRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("encoded password");

        authenticationService.register(authRequest);

        verify(passwordEncoder).encode("example password");

        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("email@example.com") &&
                        user.getPassword().equals("encoded password")
        ));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(authRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(authRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT)
                );

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(authRequest.getPassword(), mockUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(mockUser.getEmail())).thenReturn("jwt token");

        AuthResponse response = authenticationService.login(authRequest);

        assertThat(response.getToken()).isEqualTo("jwt token");

        verify(jwtService).generateToken("email@example.com");
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(authRequest.getPassword(), mockUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(authRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED)
                );
    }

    @Test
    void login_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(authRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED)
                );
    }
}
