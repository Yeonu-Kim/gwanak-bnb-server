package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.room.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

    // getRooms 목록 조회 시 category, host N+1 방지
    @Override
    @EntityGraph(attributePaths = {"category", "host"})
    Page<Room> findAll(Specification<Room> spec, Pageable pageable);

    // getRoom 단건 상세 조회 시 host, category, images, discounts 추가 쿼리 방지
    @EntityGraph(attributePaths = {"host", "category", "images", "discounts"})
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    java.util.Optional<Room> findByIdWithDetails(@Param("id") UUID id);

    boolean existsByIdAndHostLoginId(UUID id, String loginId);

    long countByHostId(UUID hostId);

    @Query("SELECT COALESCE(AVG(r.averageScore), 0.0) FROM Room r WHERE r.host.id = :hostId")
    double findAverageScoreByHostId(@Param("hostId") UUID hostId);
}
