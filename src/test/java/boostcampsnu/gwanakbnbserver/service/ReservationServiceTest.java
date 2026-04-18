package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;
import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.common.PageResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCancelResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCreateRequest;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCreateResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationSummaryResponse;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.ReservationRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock RoomRepository roomRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ReservationService reservationService;

    private static final String GUEST_LOGIN_ID = "guest@test.com";
    private static final String OTHER_LOGIN_ID = "other@test.com";

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

    // ──────────────────────── create ────────────────────────

    @Test
    @DisplayName("예약 생성 성공 - 가격과 스냅샷이 올바르게 계산된다")
    void create_success_calculatesCorrectPrice() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(4, BigDecimal.valueOf(100000));
        User guest = buildUser(GUEST_LOGIN_ID);
        ReservationCreateRequest request = new ReservationCreateRequest(
                roomId, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 6), 2);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(reservationRepository.countOverlapping(any(), any(), any(), any())).willReturn(0L);
        given(userRepository.findByLoginId(GUEST_LOGIN_ID)).willReturn(Optional.of(guest));
        given(reservationRepository.save(any())).willAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
            return r;
        });

        ReservationCreateResponse response = reservationService.create(request);

        // 5박 × 100,000 = 500,000원
        assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(response.priceSnapshot().nights()).isEqualTo(5);
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("checkOut이 checkIn 이전이면 INVALID_DATE_RANGE를 던진다")
    void create_invalidDateRange_throwsError() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                UUID.randomUUID(), LocalDate.of(2025, 8, 6), LocalDate.of(2025, 8, 1), 2);

        AppException ex = assertThrows(AppException.class, () -> reservationService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("존재하지 않는 숙소이면 ROOM_NOT_FOUND를 던진다")
    void create_roomNotFound_throwsError() {
        UUID roomId = UUID.randomUUID();
        ReservationCreateRequest request = new ReservationCreateRequest(
                roomId, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 6), 2);

        given(roomRepository.findById(roomId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> reservationService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("게스트 수가 maxGuests를 초과하면 GUEST_LIMIT_EXCEEDED를 던진다")
    void create_guestLimitExceeded_throwsError() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(2, BigDecimal.valueOf(100000)); // maxGuests = 2
        ReservationCreateRequest request = new ReservationCreateRequest(
                roomId, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3), 5); // guestCount = 5

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));

        AppException ex = assertThrows(AppException.class, () -> reservationService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.GUEST_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("기간이 겹치는 예약이 있으면 ROOM_ALREADY_BOOKED를 던진다")
    void create_roomAlreadyBooked_throwsError() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom(4, BigDecimal.valueOf(100000));
        ReservationCreateRequest request = new ReservationCreateRequest(
                roomId, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 6), 2);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(reservationRepository.countOverlapping(any(), any(), any(), any())).willReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> reservationService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ROOM_ALREADY_BOOKED);
    }

    // ──────────────────────── getMyReservations ────────────────────────

    @Test
    @DisplayName("status 필터 없이 내 예약 목록을 조회한다")
    void getMyReservations_noFilter_returnsAll() {
        Reservation r = buildReservation(GUEST_LOGIN_ID, ReservationStatus.PENDING);
        given(reservationRepository.findByUserLoginId(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(r)));

        PageResponse<ReservationSummaryResponse> result =
                reservationService.getMyReservations(null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("status 필터로 내 예약 목록을 조회한다")
    void getMyReservations_withStatusFilter_returnsFiltered() {
        Reservation r = buildReservation(GUEST_LOGIN_ID, ReservationStatus.CONFIRMED);
        given(reservationRepository.findByUserLoginIdAndStatus(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(r)));

        PageResponse<ReservationSummaryResponse> result =
                reservationService.getMyReservations(ReservationStatus.CONFIRMED, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    // ──────────────────────── cancel ────────────────────────

    @Test
    @DisplayName("예약 취소 성공 - 상태가 CANCELLED로 변경된다")
    void cancel_success() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, ReservationStatus.PENDING);
        // checkIn을 내일 이후로 설정
        ReflectionTestUtils.setField(reservation, "checkIn", LocalDate.now().plusDays(3));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        ReservationCancelResponse response = reservationService.cancel(reservationId);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("존재하지 않는 예약 ID이면 RESERVATION_NOT_FOUND를 던진다")
    void cancel_notFound_throwsError() {
        UUID reservationId = UUID.randomUUID();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> reservationService.cancel(reservationId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 예약이 아니면 FORBIDDEN을 던진다")
    void cancel_notOwner_throwsForbiddenError() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = buildReservation(OTHER_LOGIN_ID, ReservationStatus.PENDING);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        AppException ex = assertThrows(AppException.class, () -> reservationService.cancel(reservationId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("이미 취소된 예약이면 ALREADY_CANCELLED를 던진다")
    void cancel_alreadyCancelled_throwsError() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, ReservationStatus.CANCELLED);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        AppException ex = assertThrows(AppException.class, () -> reservationService.cancel(reservationId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("체크인 당일 이전이 아니면 CANCEL_NOT_ALLOWED를 던진다")
    void cancel_checkInTodayOrPast_throwsError() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, ReservationStatus.CONFIRMED);
        // checkIn을 오늘로 설정
        ReflectionTestUtils.setField(reservation, "checkIn", LocalDate.now());

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        AppException ex = assertThrows(AppException.class, () -> reservationService.cancel(reservationId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANCEL_NOT_ALLOWED);
    }

    // ──────────────────────── helpers ────────────────────────

    private User buildUser(String loginId) {
        User user = User.builder().loginId(loginId).password("pw")
                .name("유저").userType(UserType.GUEST).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Room buildRoom(int maxGuests, BigDecimal price) {
        User host = User.builder().loginId("host@test.com").password("pw")
                .name("호스트").userType(UserType.HOST).build();
        ReflectionTestUtils.setField(host, "id", UUID.randomUUID());
        Room room = Room.builder().name("숙소").maxGuests(maxGuests)
                .pricePerNight(price).host(host).build();
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(room, "discounts", new ArrayList<>());
        return room;
    }

    private Reservation buildReservation(String guestLoginId, ReservationStatus status) {
        User guest = buildUser(guestLoginId);
        Room room = buildRoom(4, BigDecimal.valueOf(100000));
        Reservation reservation = Reservation.builder()
                .room(room).user(guest)
                .checkIn(LocalDate.now().plusDays(5))
                .checkOut(LocalDate.now().plusDays(10))
                .guestCount(2).totalPrice(BigDecimal.valueOf(500000))
                .priceSnapshot(new PriceSnapshot(BigDecimal.valueOf(100000), 5, BigDecimal.ZERO))
                .build();
        ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(reservation, "status", status);
        return reservation;
    }
}
