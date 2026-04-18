package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.auth.LoginRequest;
import boostcampsnu.gwanakbnbserver.dto.auth.LoginResponse;
import boostcampsnu.gwanakbnbserver.dto.auth.RegisterRequest;
import boostcampsnu.gwanakbnbserver.dto.auth.RegisterResponse;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.UserRepository;
import boostcampsnu.gwanakbnbserver.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    // ──────────────────────── register ────────────────────────

    @Test
    @DisplayName("회원가입 성공 시 RegisterResponse를 반환한다")
    void register_success() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "홍길동", UserType.GUEST);
        User savedUser = buildUser("user@test.com", UserType.GUEST);

        given(userRepository.existsByLoginId("user@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertThat(response.loginId()).isEqualTo("user@test.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.userType()).isEqualTo(UserType.GUEST);
    }

    @Test
    @DisplayName("중복 loginId로 회원가입 시 DUPLICATE_LOGIN_ID 에러코드를 던진다")
    void register_duplicateLoginId_throwsDuplicateLoginIdError() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "홍길동", null);
        given(userRepository.existsByLoginId("user@test.com")).willReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("userType 미입력 시 기본값 GUEST로 저장된다")
    void register_nullUserType_defaultsToGuest() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "홍길동", null);
        User savedUser = buildUser("user@test.com", UserType.GUEST);

        given(userRepository.existsByLoginId(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertThat(response.userType()).isEqualTo(UserType.GUEST);
    }

    // ──────────────────────── login ────────────────────────

    @Test
    @DisplayName("로그인 성공 시 accessToken을 포함한 LoginResponse를 반환한다")
    void login_success() {
        LoginRequest request = new LoginRequest("user@test.com", "password123");
        User user = buildUser("user@test.com", UserType.GUEST);

        given(userRepository.findByLoginId("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("user@test.com")).willReturn("jwt-token");
        given(jwtUtil.getExpirationSeconds()).willReturn(3600L);

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("존재하지 않는 loginId로 로그인 시 INVALID_CREDENTIALS 에러코드를 던진다")
    void login_userNotFound_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("nobody@test.com", "password123");
        given(userRepository.findByLoginId("nobody@test.com")).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 INVALID_CREDENTIALS 에러코드를 던진다")
    void login_wrongPassword_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("user@test.com", "wrongpassword");
        User user = buildUser("user@test.com", UserType.GUEST);

        given(userRepository.findByLoginId("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpassword", "encoded-password")).willReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    // ──────────────────────── helpers ────────────────────────

    private User buildUser(String loginId, UserType userType) {
        User user = User.builder().loginId(loginId).password("encoded-password")
                .name("홍길동").userType(userType).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
