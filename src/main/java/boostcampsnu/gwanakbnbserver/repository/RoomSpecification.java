package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class RoomSpecification {

    private RoomSpecification() {}

    public static Specification<Room> withFilters(
            UUID categoryId,
            UUID regionId,
            Integer guests,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate checkIn,
            LocalDate checkOut,
            Collection<UUID> keywordMatchedIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 위치 필터: Region의 바운딩박스(latMin/latMax/lngMin/lngMax) 안에 숙소 좌표가 포함되는지 검증
            if (regionId != null) {
                var regionJoin = root.join("region", JoinType.INNER);
                predicates.add(cb.equal(regionJoin.get("id"), regionId));

                // 좌표 미설정 숙소는 region 배정 기준으로만 포함, 좌표가 있으면 바운딩박스 안에 있어야 함
                Predicate noCoords = cb.or(
                        cb.isNull(root.get("latitude")),
                        cb.isNull(root.get("longitude"))
                );
                Predicate withinBounds = cb.and(
                        cb.isNotNull(regionJoin.get("latMin")),
                        cb.between(root.<Double>get("latitude"),
                                regionJoin.<Double>get("latMin"),
                                regionJoin.<Double>get("latMax")),
                        cb.between(root.<Double>get("longitude"),
                                regionJoin.<Double>get("lngMin"),
                                regionJoin.<Double>get("lngMax"))
                );
                predicates.add(cb.or(noCoords, withinBounds));
            }

            if (guests != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxGuests"), guests));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
            }

            // 가용 날짜 필터: checkIn~checkOut과 겹치는 활성 예약이 없는 숙소만 반환
            if (checkIn != null && checkOut != null) {
                var bookedSub = query.subquery(Integer.class);
                var resRoot = bookedSub.from(Reservation.class);
                bookedSub.select(cb.literal(1)).where(
                        cb.equal(resRoot.get("room"), root),
                        cb.notEqual(resRoot.get("status"), ReservationStatus.CANCELLED),
                        cb.lessThan(resRoot.<LocalDate>get("checkIn"), checkOut),
                        cb.greaterThan(resRoot.<LocalDate>get("checkOut"), checkIn)
                );
                predicates.add(cb.not(cb.exists(bookedSub)));
            }

            // 키워드 필터: FULLTEXT 검색으로 매칭된 ID만 허용
            if (keywordMatchedIds != null) {
                if (keywordMatchedIds.isEmpty()) {
                    return cb.disjunction(); // 매칭 결과 없음 → 빈 결과 반환
                }
                predicates.add(root.get("id").in(keywordMatchedIds));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
