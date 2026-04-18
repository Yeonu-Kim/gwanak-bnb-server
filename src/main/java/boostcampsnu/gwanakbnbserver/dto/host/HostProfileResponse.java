package boostcampsnu.gwanakbnbserver.dto.host;

import boostcampsnu.gwanakbnbserver.domain.user.HostType;
import boostcampsnu.gwanakbnbserver.domain.user.User;

import java.time.Instant;
import java.util.UUID;

public record HostProfileResponse(
        UUID id,
        String name,
        String description,
        String thumbnailUrl,
        HostType hostType,
        Instant startedAt,
        long roomCount,
        double averageScore
) {
    public static HostProfileResponse of(User user, long roomCount, double averageScore) {
        return new HostProfileResponse(
                user.getId(),
                user.getName(),
                user.getDescription(),
                user.getThumbnailUrl(),
                user.getHostType(),
                user.getHostStartedAt(),
                roomCount,
                averageScore
        );
    }
}
