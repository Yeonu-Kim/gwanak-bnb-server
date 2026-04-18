package boostcampsnu.gwanakbnbserver.dto.review;

import java.util.List;

public record RoomReviewsResponse(
        double averageScore,
        List<ReviewResponse> content,
        long totalElements
) {
}
