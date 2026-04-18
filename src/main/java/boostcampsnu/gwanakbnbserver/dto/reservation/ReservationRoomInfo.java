package boostcampsnu.gwanakbnbserver.dto.reservation;

import boostcampsnu.gwanakbnbserver.domain.room.Room;

import java.util.UUID;

public record ReservationRoomInfo(
        UUID id,
        String name,
        String thumbnailUrl
) {
    public static ReservationRoomInfo from(Room room) {
        return new ReservationRoomInfo(room.getId(), room.getName(), room.getThumbnailUrl());
    }
}
