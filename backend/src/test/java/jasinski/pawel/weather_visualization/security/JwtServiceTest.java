package jasinski.pawel.weather_visualization.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(testSecretKey);
    }

    @Test
    void generateToken_shouldCreateValidToken_whenEmailIsProvided() {

        String email = "test@example.com";

        String token = jwtService.generateToken(email);


        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtService.isTokenValid(token, email)).isTrue();
    }


    @Test
    void isTokenValid_shouldReturnFalse_whenEmailDoesNotMatch() {

        String token = jwtService.generateToken("user1@example.com");
        boolean isValid = jwtService.isTokenValid(token, "user2@example.com");

        assertThat(isValid).isFalse();
    }

    @Test
    void extractEmail_shouldThrowException_whenTokenIsMalformed() {

        String badToken = "eyJhbGciOiJIUzI1NiJ9.this.is.not.valid.token";

        assertThatThrownBy(() -> jwtService.extractEmail(badToken))
                .isInstanceOf(MalformedJwtException.class);
    }

}
