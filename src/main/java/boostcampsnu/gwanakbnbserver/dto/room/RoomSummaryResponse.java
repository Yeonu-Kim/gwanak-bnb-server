package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.Room;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomSummaryResponse(
        UUID id,
        String name,
        String thumbnailUrl,
        BigDecimal pricePerNight,
        String city,
        String country,
        int maxGuests,
        double averageScore,
        int reviewCount,
        CategoryResponse category
) {
    public static RoomSummaryResponse from(Room room) {
        return new RoomSummaryResponse(
                room.getId(),
                room.getName(),
                room.getThumbnailUrl(),
                room.getPricePerNight(),
                room.getCity(),
                room.getCountry(),
                room.getMaxGuests(),
                room.getAverageScore(),
                room.getReviewCount(),
                CategoryResponse.from(room.getCategory())
        );
    }
}
