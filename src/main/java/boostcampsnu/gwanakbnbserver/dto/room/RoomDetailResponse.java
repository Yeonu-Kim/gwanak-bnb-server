package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.Room;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomDetailResponse(
        UUID id,
        String name,
        String description,
        String shortDescription,
        int maxGuests,
        Integer maxAdults,
        Integer maxChildren,
        Integer maxPets,
        BigDecimal pricePerNight,
        BigDecimal weekendPricePerNight,
        String country,
        String city,
        String state,
        Double latitude,
        Double longitude,
        String thumbnailUrl,
        List<RoomImageResponse> images,
        CategoryResponse category,
        HostResponse host,
        List<DiscountResponse> discounts,
        double averageScore,
        int reviewCount,
        Instant createdAt
) {
    public static RoomDetailResponse from(Room room) {
        return new RoomDetailResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getShortDescription(),
                room.getMaxGuests(),
                room.getMaxAdults(),
                room.getMaxChildren(),
                room.getMaxPets(),
                room.getPricePerNight(),
                room.getWeekendPricePerNight(),
                room.getCountry(),
                room.getCity(),
                room.getState(),
                room.getLatitude(),
                room.getLongitude(),
                room.getThumbnailUrl(),
                room.getImages().stream().map(RoomImageResponse::from).toList(),
                CategoryResponse.from(room.getCategory()),
                HostResponse.from(room.getHost()),
                room.getDiscounts().stream().map(DiscountResponse::from).toList(),
                room.getAverageScore(),
                room.getReviewCount(),
                room.getCreatedAt()
        );
    }
}
