package boostcampsnu.gwanakbnbserver.dto.host;

import boostcampsnu.gwanakbnbserver.domain.user.HostType;
import boostcampsnu.gwanakbnbserver.domain.user.User;

import java.util.UUID;

public record HostRegisterResponse(UUID id, String name, HostType hostType) {
    public static HostRegisterResponse from(User user) {
        return new HostRegisterResponse(user.getId(), user.getName(), user.getHostType());
    }
}
