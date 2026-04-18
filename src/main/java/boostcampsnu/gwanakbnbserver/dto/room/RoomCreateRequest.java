package boostcampsnu.gwanakbnbserver.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomCreateRequest(
        @NotBlank @Size(max = 255) String name,
        UUID categoryId,
        UUID regionId,
        @Size(max = 255) String shortDescription,
        String description,
        @NotNull @Positive Integer maxGuests,
        Integer maxAdults,
        Integer maxChildren,
        Integer maxInfants,
        Integer maxPets,
        @NotNull @Positive BigDecimal pricePerNight,
        BigDecimal weekendPricePerNight,
        String country,
        String city,
        String state,
        Double latitude,
        Double longitude
) {
}
