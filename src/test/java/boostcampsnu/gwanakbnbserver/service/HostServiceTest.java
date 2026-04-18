package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.user.HostType;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.host.HostProfileResponse;
import boostcampsnu.gwanakbnbserver.dto.host.HostRegisterRequest;
import boostcampsnu.gwanakbnbserver.dto.host.HostRegisterResponse;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.RoomRepository;
import boostcampsnu.gwanakbnbserver.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoomRepository roomRepository;

    @InjectMocks HostService hostService;

    private static final String HOST_LOGIN_ID = "host@test.com";
    private static final String GUEST_LOGIN_ID = "guest@test.com";

    @BeforeEach
    void mockAuth() {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        given(auth.getName()).willReturn(GUEST_LOGIN_ID);
        given(ctx.getAuthentication()).willReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────── getHostProfile ────────────────────────

    @Test
    @DisplayName("호스트 프로필 조회 성공 - 숙소 수와 평균 점수를 반환한다")
    void getHostProfile_success() {
        UUID hostId = UUID.randomUUID();
        User host = buildUser(HOST_LOGIN_ID, UserType.HOST);
        ReflectionTestUtils.setField(host, "id", hostId);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(roomRepository.countByHostId(hostId)).willReturn(3L);
        given(roomRepository.findAverageScoreByHostId(hostId)).willReturn(4.5);

        HostProfileResponse response = hostService.getHostProfile(hostId);

        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.roomCount()).isEqualTo(3L);
        assertThat(response.averageScore()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("존재하지 않는 호스트 ID이면 HOST_NOT_FOUND를 던진다")
    void getHostProfile_notFound_throwsError() {
        UUID hostId = UUID.randomUUID();
        given(userRepository.findById(hostId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> hostService.getHostProfile(hostId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.HOST_NOT_FOUND);
    }

    @Test
    @DisplayName("해당 ID가 GUEST 계정이면 HOST_NOT_FOUND를 던진다")
    void getHostProfile_userIsGuest_throwsHostNotFound() {
        UUID userId = UUID.randomUUID();
        User guest = buildUser(GUEST_LOGIN_ID, UserType.GUEST);
        ReflectionTestUtils.setField(guest, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(guest));

        AppException ex = assertThrows(AppException.class, () -> hostService.getHostProfile(userId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.HOST_NOT_FOUND);
    }

    // ──────────────────────── registerHost ────────────────────────

    @Test
    @DisplayName("호스트 등록 성공 - userType이 HOST로 변경된다")
    void registerHost_success() {
        User guest = buildUser(GUEST_LOGIN_ID, UserType.GUEST);
        HostRegisterRequest request = new HostRegisterRequest("새호스트", "소개글", "thumb.png");

        given(userRepository.findByLoginId(GUEST_LOGIN_ID)).willReturn(Optional.of(guest));

        HostRegisterResponse response = hostService.registerHost(request);

        assertThat(response.hostType()).isEqualTo(HostType.HOST);
        assertThat(response.name()).isEqualTo("새호스트");
    }

    @Test
    @DisplayName("이미 HOST인 유저가 등록 시 ALREADY_HOST를 던진다")
    void registerHost_alreadyHost_throwsError() {
        User host = buildUser(GUEST_LOGIN_ID, UserType.HOST);
        HostRegisterRequest request = new HostRegisterRequest("호스트", null, null);

        given(userRepository.findByLoginId(GUEST_LOGIN_ID)).willReturn(Optional.of(host));

        AppException ex = assertThrows(AppException.class, () -> hostService.registerHost(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_HOST);
    }

    // ──────────────────────── helpers ────────────────────────

    private User buildUser(String loginId, UserType userType) {
        User user = User.builder().loginId(loginId).password("pw")
                .name("홍길동").userType(userType).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
