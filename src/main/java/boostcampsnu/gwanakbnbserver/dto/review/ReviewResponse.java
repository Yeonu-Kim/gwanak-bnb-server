package boostcampsnu.gwanakbnbserver.dto.review;

import boostcampsnu.gwanakbnbserver.domain.review.Review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        int score,
        String content,
        Instant createdAt,
        List<ReviewImageResponse> images,
        ReviewerResponse reviewer
) {
    public record ReviewerResponse(String name) {
    }

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getScore(),
                review.getContent(),
                review.getCreatedAt(),
                review.getImages().stream().map(ReviewImageResponse::from).toList(),
                new ReviewerResponse(review.getReviewer().getName())
        );
    }
}
