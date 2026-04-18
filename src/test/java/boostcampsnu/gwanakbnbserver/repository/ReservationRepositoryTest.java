package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;
import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ReservationRepositoryTest {

    @Autowired ReservationRepository reservationRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired UserRepository userRepository;

    private Room room;
    private User guest;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .loginId("host@test.com").password("pw").name("호스트").userType(UserType.HOST).build());
        guest = userRepository.save(User.builder()
                .loginId("guest@test.com").password("pw").name("게스트").userType(UserType.GUEST).build());
        room = roomRepository.save(Room.builder()
                .name("테스트숙소").maxGuests(4)
                .pricePerNight(BigDecimal.valueOf(100000)).host(host).build());
    }

    private Reservation saveReservation(LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
        Reservation reservation = Reservation.builder()
                .room(room).user(guest)
                .checkIn(checkIn).checkOut(checkOut)
                .guestCount(2)
                .totalPrice(BigDecimal.valueOf(200000))
                .priceSnapshot(new PriceSnapshot(BigDecimal.valueOf(100000), 2, BigDecimal.ZERO))
                .build();
        Reservation saved = reservationRepository.save(reservation);
        if (status != ReservationStatus.PENDING) {
            ReflectionTestUtils.setField(saved, "status", status);
            reservationRepository.save(saved);
        }
        return saved;
    }

    @Test
    @DisplayName("유저 loginId로 예약 목록을 조회한다")
    void findByUserLoginId_returnsUserReservations() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3), ReservationStatus.PENDING);
        saveReservation(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3), ReservationStatus.CONFIRMED);

        Page<Reservation> result = reservationRepository.findByUserLoginId("guest@test.com", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("상태 필터와 함께 유저 예약 목록을 조회한다")
    void findByUserLoginIdAndStatus_returnsFilteredReservations() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3), ReservationStatus.PENDING);
        saveReservation(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3), ReservationStatus.CANCELLED);

        Page<Reservation> result = reservationRepository.findByUserLoginIdAndStatus(
                "guest@test.com", ReservationStatus.PENDING, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("기간이 겹치는 예약이 있으면 countOverlapping이 1 이상을 반환한다")
    void countOverlapping_whenOverlap_returnsPositive() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 10), ReservationStatus.CONFIRMED);

        long count = reservationRepository.countOverlapping(
                room.getId(), ReservationStatus.CANCELLED,
                LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 15));

        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("기간이 겹치지 않으면 countOverlapping이 0을 반환한다")
    void countOverlapping_whenNoOverlap_returnsZero() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 5), ReservationStatus.CONFIRMED);

        long count = reservationRepository.countOverlapping(
                room.getId(), ReservationStatus.CANCELLED,
                LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 15));

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("취소된 예약은 countOverlapping에서 제외된다")
    void countOverlapping_excludesCancelledReservations() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 10), ReservationStatus.CANCELLED);

        long count = reservationRepository.countOverlapping(
                room.getId(), ReservationStatus.CANCELLED,
                LocalDate.of(2025, 7, 5), LocalDate.of(2025, 7, 15));

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("활성 상태 예약이 있으면 existsByRoomIdAndStatusIn이 true를 반환한다")
    void existsByRoomIdAndStatusIn_whenActiveExists_returnsTrue() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 5), ReservationStatus.PENDING);

        boolean exists = reservationRepository.existsByRoomIdAndStatusIn(
                room.getId(), List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("취소된 예약만 있으면 existsByRoomIdAndStatusIn이 false를 반환한다")
    void existsByRoomIdAndStatusIn_whenOnlyCancelled_returnsFalse() {
        saveReservation(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 5), ReservationStatus.CANCELLED);

        boolean exists = reservationRepository.existsByRoomIdAndStatusIn(
                room.getId(), List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        assertThat(exists).isFalse();
    }
}
