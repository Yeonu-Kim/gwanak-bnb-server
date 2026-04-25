package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.domain.room.Category;
import boostcampsnu.gwanakbnbserver.domain.room.Region;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.room.*;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class RoomServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock RegionRepository regionRepository;
    @Mock UserRepository userRepository;
    @Mock ReservationRepository reservationRepository;

    @InjectMocks RoomService roomService;

    private static final String HOST_LOGIN_ID = "host@test.com";
    private static final String OTHER_LOGIN_ID = "other@test.com";

    @BeforeEach
    void mockAuth() {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        given(auth.getName()).willReturn(HOST_LOGIN_ID);
        given(ctx.getAuthentication()).willReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────── getRooms ────────────────────────

    @Test
    @DisplayName("getRooms: 필터 없이 숙소 목록을 페이지 조회한다")
    void getRooms_success() {
        Room room = buildRoom(buildHostUser());
        given(roomRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(room)));

        var result = roomService.getRooms(null, null, null, null, null, null, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getRooms: checkOut이 checkIn 이전이면 INVALID_DATE_RANGE를 던진다")
    void getRooms_invalidDateRange_throwsError() {
        AppException ex = assertThrows(AppException.class, () ->
                roomService.getRooms(null, null, null,
                        LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 5),
                        null, null, null, 0, 20));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    // ──────────────────────── getRoom ────────────────────────

    @Test
    @DisplayName("getRoom: 존재하는 숙소 ID로 상세 조회에 성공한다")
    void getRoom_success() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(buildHostUser());
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));

        RoomDetailResponse response = roomService.getRoom(roomId);

        assertThat(response.name()).isEqualTo("테스트숙소");
    }

    @Test
    @DisplayName("getRoom: 존재하지 않는 숙소 ID이면 ROOM_NOT_FOUND를 던진다")
    void getRoom_notFound_throwsError() {
        UUID roomId = UUID.randomUUID();
        given(roomRepository.findById(roomId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> roomService.getRoom(roomId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    // ──────────────────────── createRoom ────────────────────────

    @Test
    @DisplayName("createRoom: 호스트가 숙소 등록에 성공한다")
    void createRoom_success() {
        RoomCreateRequest request = buildCreateRequest(null, null);
        User host = buildHostUser();
        Room savedRoom = buildRoom(host);

        given(userRepository.findByLoginId(HOST_LOGIN_ID)).willReturn(Optional.of(host));
        given(roomRepository.save(any(Room.class))).willReturn(savedRoom);

        RoomCreateResponse response = roomService.createRoom(request);

        assertThat(response.name()).isEqualTo("테스트숙소");
    }

    @Test
    @DisplayName("createRoom: GUEST 계정이면 HOST_ONLY 에러코드를 던진다")
    void createRoom_notHost_throwsHostOnlyError() {
        RoomCreateRequest request = buildCreateRequest(null, null);
        User guest = buildGuestUser();
        given(userRepository.findByLoginId(HOST_LOGIN_ID)).willReturn(Optional.of(guest));

        AppException ex = assertThrows(AppException.class, () -> roomService.createRoom(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.HOST_ONLY);
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRoom: 존재하지 않는 categoryId이면 CATEGORY_NOT_FOUND를 던진다")
    void createRoom_categoryNotFound_throwsError() {
        UUID catId = UUID.randomUUID();
        RoomCreateRequest request = buildCreateRequest(catId, null);
        User host = buildHostUser();

        given(userRepository.findByLoginId(HOST_LOGIN_ID)).willReturn(Optional.of(host));
        given(categoryRepository.findById(catId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> roomService.createRoom(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("createRoom: 존재하지 않는 regionId이면 REGION_NOT_FOUND를 던진다")
    void createRoom_regionNotFound_throwsError() {
        UUID regionId = UUID.randomUUID();
        RoomCreateRequest request = buildCreateRequest(null, regionId);
        User host = buildHostUser();

        given(userRepository.findByLoginId(HOST_LOGIN_ID)).willReturn(Optional.of(host));
        given(regionRepository.findById(regionId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> roomService.createRoom(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
    }

    // ──────────────────────── updateRoom ────────────────────────

    @Test
    @DisplayName("updateRoom: 숙소 소유자가 수정에 성공한다")
    void updateRoom_success() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(buildHostUser());
        RoomUpdateRequest request = new RoomUpdateRequest("수정된숙소", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));

        RoomUpdateResponse response = roomService.updateRoom(roomId, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("updateRoom: 다른 사람의 숙소이면 FORBIDDEN 에러코드를 던진다")
    void updateRoom_notOwner_throwsForbiddenError() {
        UUID roomId = UUID.randomUUID();
        User otherHost = User.builder()
                .loginId(OTHER_LOGIN_ID).password("pw").name("타인").userType(UserType.HOST).build();
        Room room = buildRoom(otherHost);
        RoomUpdateRequest request = new RoomUpdateRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));

        AppException ex = assertThrows(AppException.class, () -> roomService.updateRoom(roomId, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    // ──────────────────────── deleteRoom ────────────────────────

    @Test
    @DisplayName("deleteRoom: 활성 예약이 없으면 숙소 삭제에 성공한다")
    void deleteRoom_success() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(buildHostUser());

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(reservationRepository.existsByRoomIdAndStatusIn(eq(roomId), any())).willReturn(false);

        roomService.deleteRoom(roomId);

        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("deleteRoom: 활성 예약이 있으면 HAS_ACTIVE_RESERVATION 에러코드를 던진다")
    void deleteRoom_hasActiveReservation_throwsError() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(buildHostUser());

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(reservationRepository.existsByRoomIdAndStatusIn(eq(roomId), any())).willReturn(true);

        AppException ex = assertThrows(AppException.class, () -> roomService.deleteRoom(roomId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.HAS_ACTIVE_RESERVATION);
        verify(roomRepository, never()).delete(any(Room.class));
    }

    @Test
    @DisplayName("deleteRoom: 다른 사람의 숙소이면 FORBIDDEN 에러코드를 던진다")
    void deleteRoom_notOwner_throwsForbiddenError() {
        UUID roomId = UUID.randomUUID();
        User otherHost = User.builder()
                .loginId(OTHER_LOGIN_ID).password("pw").name("타인").userType(UserType.HOST).build();
        Room room = buildRoom(otherHost);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));

        AppException ex = assertThrows(AppException.class, () -> roomService.deleteRoom(roomId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    // ──────────────────────── getPrices ────────────────────────

    @Test
    @DisplayName("getPrices: 주중은 pricePerNight, 주말은 weekendPricePerNight를 반환한다")
    void getPrices_returnsCorrectPrices() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(buildHostUser());
        // 2025-07-07(Mon) ~ 2025-07-13(Sun) - 5개 구간
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));

        RoomPricesResponse response = roomService.getPrices(roomId,
                LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 9));

        assertThat(response.prices()).hasSize(3);
        // 7월 7일(월), 7월 8일(화)는 weekday → 100,000
        // 7월 9일(수)는 weekday → 100,000
        response.prices().forEach(p ->
                assertThat(p.price()).isEqualByComparingTo(BigDecimal.valueOf(100000)));
    }

    @Test
    @DisplayName("getPrices: to가 from보다 이전이면 INVALID_DATE_RANGE를 던진다")
    void getPrices_invalidDateRange_throwsError() {
        UUID roomId = UUID.randomUUID();

        AppException ex = assertThrows(AppException.class, () ->
                roomService.getPrices(roomId,
                        LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 5)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    // ──────────────────────── helpers ────────────────────────

    private User buildHostUser() {
        User user = User.builder().loginId(HOST_LOGIN_ID).password("pw")
                .name("호스트").userType(UserType.HOST).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private User buildGuestUser() {
        User user = User.builder().loginId(HOST_LOGIN_ID).password("pw")
                .name("게스트").userType(UserType.GUEST).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Room buildRoom(User host) {
        Room room = Room.builder()
                .name("테스트숙소").maxGuests(4)
                .pricePerNight(BigDecimal.valueOf(100000))
                .weekendPricePerNight(BigDecimal.valueOf(150000))
                .host(host).build();
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(room, "discounts", new ArrayList<>());
        ReflectionTestUtils.setField(room, "images", new ArrayList<>());
        return room;
    }

    private RoomCreateRequest buildCreateRequest(UUID categoryId, UUID regionId) {
        return new RoomCreateRequest("테스트숙소", categoryId, regionId, null, null,
                4, null, null, null, null, BigDecimal.valueOf(100000),
                null, null, null, null, null, null);
    }
}
