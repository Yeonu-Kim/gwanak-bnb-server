package boostcampsnu.gwanakbnbserver.dto.review;

import boostcampsnu.gwanakbnbserver.domain.review.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewCreateResponse(UUID id, int score, String content, Instant createdAt) {
    public static ReviewCreateResponse from(Review review) {
        return new ReviewCreateResponse(review.getId(), review.getScore(), review.getContent(), review.getCreatedAt());
    }
}
