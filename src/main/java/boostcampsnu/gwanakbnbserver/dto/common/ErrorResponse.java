package boostcampsnu.gwanakbnbserver.dto.common;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}
