package boostcampsnu.gwanakbnbserver.dto.review;

import boostcampsnu.gwanakbnbserver.domain.review.ReviewImage;

public record ReviewImageResponse(String url, String caption, int orderNum) {
    public static ReviewImageResponse from(ReviewImage image) {
        return new ReviewImageResponse(image.getUrl(), image.getCaption(), image.getOrderNum());
    }
}
