package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;
import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.domain.review.Review;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ReviewRepositoryTest {

    @Autowired ReviewRepository reviewRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired UserRepository userRepository;

    private Room room;
    private User guest;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .loginId("host@test.com").password("pw").name("호스트").userType(UserType.HOST).build());
        guest = userRepository.save(User.builder()
                .loginId("guest@test.com").password("pw").name("게스트").userType(UserType.GUEST).build());
        room = roomRepository.save(Room.builder()
                .name("테스트숙소").maxGuests(4)
                .pricePerNight(BigDecimal.valueOf(100000)).host(host).build());
        reservation = reservationRepository.save(Reservation.builder()
                .room(room).user(guest)
                .checkIn(LocalDate.of(2025, 6, 1)).checkOut(LocalDate.of(2025, 6, 5))
                .guestCount(2).totalPrice(BigDecimal.valueOf(400000))
                .priceSnapshot(new PriceSnapshot(BigDecimal.valueOf(100000), 4, BigDecimal.ZERO))
                .build());
        Reservation cancelled = reservationRepository.save(Reservation.builder()
                .room(room).user(guest)
                .checkIn(LocalDate.of(2025, 5, 1)).checkOut(LocalDate.of(2025, 5, 3))
                .guestCount(1).totalPrice(BigDecimal.valueOf(200000))
                .priceSnapshot(new PriceSnapshot(BigDecimal.valueOf(100000), 2, BigDecimal.ZERO))
                .build());
        ReflectionTestUtils.setField(cancelled, "status", ReservationStatus.CANCELLED);
        reservationRepository.save(cancelled);
    }

    @Test
    @DisplayName("리뷰 저장 후 예약 ID로 존재 여부를 확인한다")
    void existsByReservationId_whenReviewExists_returnsTrue() {
        reviewRepository.save(Review.builder()
                .reservation(reservation).room(room).reviewer(guest).score(5).content("좋아요").build());

        boolean exists = reviewRepository.existsByReservationId(reservation.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("리뷰가 없으면 existsByReservationId가 false를 반환한다")
    void existsByReservationId_whenNoReview_returnsFalse() {
        boolean exists = reviewRepository.existsByReservationId(reservation.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("숙소 ID로 리뷰 목록을 페이지 조회한다")
    void findByRoomId_returnsPaginatedReviews() {
        reviewRepository.save(Review.builder()
                .reservation(reservation).room(room).reviewer(guest).score(5).content("최고").build());

        Page<Review> page = reviewRepository.findByRoomId(room.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("숙소 ID로 평균 점수를 계산한다")
    void findAverageScoreByRoomId_returnsCorrectAverage() {
        // 두 번째 예약/리뷰 생성
        Reservation reservation2 = reservationRepository.save(Reservation.builder()
                .room(room).user(guest)
                .checkIn(LocalDate.of(2025, 7, 1)).checkOut(LocalDate.of(2025, 7, 3))
                .guestCount(2).totalPrice(BigDecimal.valueOf(200000))
                .priceSnapshot(new PriceSnapshot(BigDecimal.valueOf(100000), 2, BigDecimal.ZERO))
                .build());
        reviewRepository.save(Review.builder()
                .reservation(reservation).room(room).reviewer(guest).score(4).content("좋아요").build());
        reviewRepository.save(Review.builder()
                .reservation(reservation2).room(room).reviewer(guest).score(2).content("별로에요").build());

        Double avg = reviewRepository.findAverageScoreByRoomId(room.getId());

        assertThat(avg).isEqualTo(3.0); // (4 + 2) / 2
    }

    @Test
    @DisplayName("리뷰가 없을 때 평균 점수는 null이다")
    void findAverageScoreByRoomId_whenNoReviews_returnsNull() {
        Double avg = reviewRepository.findAverageScoreByRoomId(room.getId());

        assertThat(avg).isNull();
    }

    @Test
    @DisplayName("숙소 ID로 리뷰 수를 카운트한다")
    void countByRoomId_returnsCorrectCount() {
        reviewRepository.save(Review.builder()
                .reservation(reservation).room(room).reviewer(guest).score(5).content("좋아요").build());

        int count = reviewRepository.countByRoomId(room.getId());

        assertThat(count).isEqualTo(1);
    }
}
