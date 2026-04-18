package boostcampsnu.gwanakbnbserver.dto.reservation;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;

import java.math.BigDecimal;

public record PriceSnapshotResponse(
        BigDecimal basePrice,
        int nights,
        BigDecimal discountRate
) {
    public static PriceSnapshotResponse from(PriceSnapshot snapshot) {
        return new PriceSnapshotResponse(
                snapshot.getBasePrice(),
                snapshot.getNights(),
                snapshot.getDiscountRate()
        );
    }
}
