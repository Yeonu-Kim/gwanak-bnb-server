package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.RoomImage;

import java.util.UUID;

public record RoomImageResponse(
        UUID id,
        String url,
        String caption,
        int orderNum
) {
    public static RoomImageResponse from(RoomImage image) {
        return new RoomImageResponse(image.getId(), image.getUrl(), image.getCaption(), image.getOrderNum());
    }
}
