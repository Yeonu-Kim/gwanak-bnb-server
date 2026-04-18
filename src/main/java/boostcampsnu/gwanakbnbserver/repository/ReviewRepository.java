package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByReservationId(UUID reservationId);

    // getRoomReviews 목록 조회 시 reviewer, images N+1 방지
    @EntityGraph(attributePaths = {"reviewer", "images"})
    Page<Review> findByRoomId(UUID roomId, Pageable pageable);

    @Query("SELECT AVG(r.score) FROM Review r WHERE r.room.id = :roomId")
    Double findAverageScoreByRoomId(@Param("roomId") UUID roomId);

    int countByRoomId(UUID roomId);
}
