package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.Room;

import java.time.Instant;
import java.util.UUID;

public record RoomCreateResponse(
        UUID id,
        String name,
        Instant createdAt
) {
    public static RoomCreateResponse from(Room room) {
        return new RoomCreateResponse(room.getId(), room.getName(), room.getCreatedAt());
    }
}
