package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.user.HostType;
import boostcampsnu.gwanakbnbserver.domain.user.User;

import java.time.Instant;
import java.util.UUID;

public record HostResponse(
        UUID id,
        String name,
        String thumbnailUrl,
        HostType hostType,
        Instant startedAt
) {
    public static HostResponse from(User user) {
        return new HostResponse(
                user.getId(),
                user.getName(),
                user.getThumbnailUrl(),
                user.getHostType(),
                user.getHostStartedAt()
        );
    }
}
