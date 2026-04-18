package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.Room;

import java.time.Instant;
import java.util.UUID;

public record RoomUpdateResponse(
        UUID id,
        String name,
        Instant updatedAt
) {
    public static RoomUpdateResponse from(Room room) {
        return new RoomUpdateResponse(room.getId(), room.getName(), room.getUpdatedAt());
    }
}
