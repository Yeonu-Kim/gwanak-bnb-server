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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

    // getRooms 목록 조회 시 category, host N+1 방지
    @Override
    @EntityGraph(attributePaths = {"category", "host"})
    Page<Room> findAll(Specification<Room> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"host", "category", "images"})
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithImages(@Param("id") UUID id);

    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.discounts WHERE r.id = :id")
    Optional<Room> findByIdWithDiscounts(@Param("id") UUID id);

    /**
     * MySQL FULLTEXT (ngram parser) 기반 시맨틱 유사도 검색.
     * 관련도 순으로 최대 200건의 id hex 문자열을 반환한다.
     *
     * DDL (최초 1회 실행 필요):
     *   ALTER TABLE rooms
     *     ADD FULLTEXT INDEX ft_room_search (name, short_description, description)
     *     WITH PARSER ngram;
     *
     * 주의: H2 테스트 환경에서는 동작하지 않으므로 서비스 레이어에서 mock 처리한다.
     */
    @Query(value = """
            SELECT LOWER(HEX(id))
            FROM rooms
            WHERE MATCH(name, short_description, description)
                  AGAINST(:keyword IN NATURAL LANGUAGE MODE) > 0
            ORDER BY MATCH(name, short_description, description)
                     AGAINST(:keyword IN NATURAL LANGUAGE MODE) DESC
            LIMIT 200
            """, nativeQuery = true)
    List<String> findIdsByKeyword(@Param("keyword") String keyword);

    boolean existsByIdAndHostLoginId(UUID id, String loginId);

    long countByHostId(UUID hostId);

    @Query("SELECT COALESCE(AVG(r.averageScore), 0.0) FROM Room r WHERE r.host.id = :hostId")
    double findAverageScoreByHostId(@Param("hostId") UUID hostId);
}
