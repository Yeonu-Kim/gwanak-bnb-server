package boostcampsnu.gwanakbnbserver.dto.auth;

import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String loginId,
        String name,
        UserType userType,
        Instant createdAt
) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getUserType(),
                user.getCreatedAt()
        );
    }
}
