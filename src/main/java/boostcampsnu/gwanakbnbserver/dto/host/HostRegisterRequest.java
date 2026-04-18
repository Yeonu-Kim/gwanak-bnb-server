package boostcampsnu.gwanakbnbserver.dto.host;

import jakarta.validation.constraints.NotBlank;

public record HostRegisterRequest(
        @NotBlank String name,
        String description,
        String thumbnailUrl
) {
}
