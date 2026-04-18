package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // getMyReservations 목록 조회 시 room N+1 방지
    @EntityGraph(attributePaths = {"room", "user"})
    Page<Reservation> findByUserLoginId(String loginId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "user"})
    Page<Reservation> findByUserLoginIdAndStatus(String loginId, ReservationStatus status, Pageable pageable);

    // cancel, createReview 단건 조회 시 user, room 추가 쿼리 방지
    @EntityGraph(attributePaths = {"room", "user"})
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    java.util.Optional<Reservation> findByIdWithRoomAndUser(@Param("id") UUID id);

    @Query("SELECT COUNT(r) FROM Reservation r " +
           "WHERE r.room.id = :roomId " +
           "AND r.status <> :cancelled " +
           "AND r.checkIn < :checkOut AND r.checkOut > :checkIn")
    long countOverlapping(@Param("roomId") UUID roomId,
                          @Param("cancelled") ReservationStatus cancelled,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut);

    boolean existsByRoomIdAndStatusIn(UUID roomId, List<ReservationStatus> statuses);
}
