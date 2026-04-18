package boostcampsnu.gwanakbnbserver.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReviewUpdateRequest(
        @Min(1) @Max(5) Integer score,
        String content
) {
}
