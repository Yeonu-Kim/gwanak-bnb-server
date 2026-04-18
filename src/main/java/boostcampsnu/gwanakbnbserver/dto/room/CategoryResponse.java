package boostcampsnu.gwanakbnbserver.dto.room;

import boostcampsnu.gwanakbnbserver.domain.room.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String iconUrl
) {
    public static CategoryResponse from(Category category) {
        if (category == null) return null;
        return new CategoryResponse(category.getId(), category.getName(), category.getIconUrl());
    }
}
