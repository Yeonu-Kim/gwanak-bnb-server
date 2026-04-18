package boostcampsnu.gwanakbnbserver.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCreateRequest(
        @NotNull UUID reservationId,
        @NotNull @Min(1) @Max(5) Integer score,
        String content
) {
}
