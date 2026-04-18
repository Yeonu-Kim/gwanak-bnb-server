package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;
import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.domain.review.Review;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.review.*;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.ReservationRepository;
import boostcampsnu.gwanakbnbserver.repository.ReviewRepository;
import boostcampsnu.gwanakbnbserver.repository.RoomRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock RoomRepository roomRepository;

    @InjectMocks ReviewService reviewService;

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

    // ──────────────────────── createReview ────────────────────────

    @Test
    @DisplayName("리뷰 작성 성공 - 체크아웃 이후, 본인 예약, 중복 없음")
    void createReview_success() {
        UUID reservationId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(reservationId, 5, "최고의 숙소!");
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, LocalDate.now().minusDays(1));
        Review savedReview = buildReview(reservation, GUEST_LOGIN_ID, 5);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(reviewRepository.existsByReservationId(reservationId)).willReturn(false);
        given(reviewRepository.save(any())).willReturn(savedReview);
        given(reviewRepository.findAverageScoreByRoomId(any())).willReturn(5.0);
        given(reviewRepository.countByRoomId(any())).willReturn(1);

        ReviewCreateResponse response = reviewService.createReview(request);

        assertThat(response.score()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("최고의 숙소!");
    }

    @Test
    @DisplayName("예약이 존재하지 않으면 RESERVATION_NOT_FOUND를 던진다")
    void createReview_reservationNotFound_throwsError() {
        UUID reservationId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(reservationId, 5, "좋아요");
        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> reviewService.createReview(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 예약이 아니면 FORBIDDEN을 던진다")
    void createReview_notOwner_throwsForbiddenError() {
        UUID reservationId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(reservationId, 5, "좋아요");
        Reservation reservation = buildReservation(OTHER_LOGIN_ID, LocalDate.now().minusDays(1));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        AppException ex = assertThrows(AppException.class, () -> reviewService.createReview(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("체크아웃 전이면 CHECKOUT_REQUIRED를 던진다")
    void createReview_beforeCheckout_throwsError() {
        UUID reservationId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(reservationId, 5, "좋아요");
        // checkOut이 내일: 아직 체크아웃 전
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, LocalDate.now().plusDays(1));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        AppException ex = assertThrows(AppException.class, () -> reviewService.createReview(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHECKOUT_REQUIRED);
    }

    @Test
    @DisplayName("이미 리뷰가 존재하면 REVIEW_ALREADY_EXISTS를 던진다")
    void createReview_alreadyExists_throwsError() {
        UUID reservationId = UUID.randomUUID();
        ReviewCreateRequest request = new ReviewCreateRequest(reservationId, 5, "좋아요");
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, LocalDate.now().minusDays(1));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(reviewRepository.existsByReservationId(reservationId)).willReturn(true);

        AppException ex = assertThrows(AppException.class, () -> reviewService.createReview(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // ──────────────────────── getRoomReviews ────────────────────────

    @Test
    @DisplayName("숙소 리뷰 목록 조회 성공 - 평균 점수와 리뷰 수를 반환한다")
    void getRoomReviews_success() {
        UUID roomId = UUID.randomUUID();
        Room room = buildRoom();
        ReflectionTestUtils.setField(room, "id", roomId);
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, LocalDate.now().minusDays(1));
        Review review = buildReview(reservation, GUEST_LOGIN_ID, 4);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(reviewRepository.findByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(review)));
        given(reviewRepository.findAverageScoreByRoomId(roomId)).willReturn(4.0);

        RoomReviewsResponse response = reviewService.getRoomReviews(roomId, 0, 10);

        assertThat(response.averageScore()).isEqualTo(4.0);
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 숙소 ID이면 ROOM_NOT_FOUND를 던진다")
    void getRoomReviews_roomNotFound_throwsError() {
        UUID roomId = UUID.randomUUID();
        given(roomRepository.findById(roomId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> reviewService.getRoomReviews(roomId, 0, 10));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    // ──────────────────────── updateReview ────────────────────────

    @Test
    @DisplayName("리뷰 수정 성공 - 점수와 내용이 변경된다")
    void updateReview_success() {
        UUID reviewId = UUID.randomUUID();
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, LocalDate.now().minusDays(1));
        Review review = buildReview(reservation, GUEST_LOGIN_ID, 3);
        ReflectionTestUtils.setField(review, "id", reviewId);
        ReviewUpdateRequest request = new ReviewUpdateRequest(5, "다시 생각해보니 최고에요");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.findAverageScoreByRoomId(any())).willReturn(5.0);
        given(reviewRepository.countByRoomId(any())).willReturn(1);

        ReviewUpdateResponse response = reviewService.updateReview(reviewId, request);

        assertThat(response.score()).isEqualTo(5);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 ID이면 REVIEW_NOT_FOUND를 던진다")
    void updateReview_notFound_throwsError() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> reviewService.updateReview(reviewId, new ReviewUpdateRequest(4, "수정")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 리뷰가 아니면 FORBIDDEN을 던진다")
    void updateReview_notReviewer_throwsForbiddenError() {
        UUID reviewId = UUID.randomUUID();
        Reservation reservation = buildReservation(OTHER_LOGIN_ID, LocalDate.now().minusDays(1));
        Review review = buildReview(reservation, OTHER_LOGIN_ID, 3);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        AppException ex = assertThrows(AppException.class,
                () -> reviewService.updateReview(reviewId, new ReviewUpdateRequest(5, "수정")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    // ──────────────────────── deleteReview ────────────────────────

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() {
        UUID reviewId = UUID.randomUUID();
        Reservation reservation = buildReservation(GUEST_LOGIN_ID, LocalDate.now().minusDays(1));
        Review review = buildReview(reservation, GUEST_LOGIN_ID, 5);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(reviewRepository.findAverageScoreByRoomId(any())).willReturn(null);
        given(reviewRepository.countByRoomId(any())).willReturn(0);

        reviewService.deleteReview(reviewId);

        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 ID이면 REVIEW_NOT_FOUND를 던진다")
    void deleteReview_notFound_throwsError() {
        UUID reviewId = UUID.randomUUID();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> reviewService.deleteReview(reviewId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 리뷰가 아니면 FORBIDDEN을 던진다")
    void deleteReview_notReviewer_throwsForbiddenError() {
        UUID reviewId = UUID.randomUUID();
        Reservation reservation = buildReservation(OTHER_LOGIN_ID, LocalDate.now().minusDays(1));
        Review review = buildReview(reservation, OTHER_LOGIN_ID, 5);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        AppException ex = assertThrows(AppException.class, () -> reviewService.deleteReview(reviewId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    // ──────────────────────── helpers ────────────────────────

    private Room buildRoom() {
        User host = User.builder().loginId("host@test.com").password("pw")
                .name("호스트").userType(UserType.HOST).build();
        ReflectionTestUtils.setField(host, "id", UUID.randomUUID());
        Room room = Room.builder().name("숙소").maxGuests(4)
                .pricePerNight(BigDecimal.valueOf(100000)).host(host).build();
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(room, "images", new ArrayList<>());
        ReflectionTestUtils.setField(room, "discounts", new ArrayList<>());
        return room;
    }

    private Reservation buildReservation(String guestLoginId, LocalDate checkOut) {
        User guest = User.builder().loginId(guestLoginId).password("pw")
                .name("게스트").userType(UserType.GUEST).build();
        ReflectionTestUtils.setField(guest, "id", UUID.randomUUID());
        Room room = buildRoom();
        Reservation r = Reservation.builder()
                .room(room).user(guest)
                .checkIn(checkOut.minusDays(5)).checkOut(checkOut)
                .guestCount(2).totalPrice(BigDecimal.valueOf(500000))
                .priceSnapshot(new PriceSnapshot(BigDecimal.valueOf(100000), 5, BigDecimal.ZERO))
                .build();
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(r, "status", ReservationStatus.CONFIRMED);
        return r;
    }

    private Review buildReview(Reservation reservation, String reviewerLoginId, int score) {
        User reviewer = User.builder().loginId(reviewerLoginId).password("pw")
                .name("리뷰어").userType(UserType.GUEST).build();
        ReflectionTestUtils.setField(reviewer, "id", UUID.randomUUID());
        Review review = Review.builder()
                .reservation(reservation)
                .room(reservation.getRoom())
                .reviewer(reviewer)
                .score(score)
                .content("테스트 리뷰")
                .build();
        ReflectionTestUtils.setField(review, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(review, "images", new ArrayList<>());
        return review;
    }
}
