package boostcampsnu.gwanakbnbserver.dto.review;

import boostcampsnu.gwanakbnbserver.domain.review.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewUpdateResponse(UUID id, int score, String content, Instant updatedAt) {
    public static ReviewUpdateResponse from(Review review) {
        return new ReviewUpdateResponse(review.getId(), review.getScore(), review.getContent(), review.getUpdatedAt());
    }
}
