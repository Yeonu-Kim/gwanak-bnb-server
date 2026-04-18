package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.RoomDiscount;

import java.math.BigDecimal;

public record DiscountResponse(
        int minNights,
        BigDecimal discountRate
) {
    public static DiscountResponse from(RoomDiscount discount) {
        return new DiscountResponse(discount.getMinNights(), discount.getDiscountRate());
    }
}
