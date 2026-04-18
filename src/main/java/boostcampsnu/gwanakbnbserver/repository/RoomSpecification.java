package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.room.Room;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoomSpecification {

    private RoomSpecification() {
    }

    public static Specification<Room> withFilters(UUID categoryId, UUID regionId,
                                                   Integer guests,
                                                   BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (regionId != null) {
                predicates.add(cb.equal(root.get("region").get("id"), regionId));
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
