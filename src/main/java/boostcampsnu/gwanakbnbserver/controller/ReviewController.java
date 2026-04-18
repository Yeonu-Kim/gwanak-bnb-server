package boostcampsnu.gwanakbnbserver.controller;

import boostcampsnu.gwanakbnbserver.dto.review.ReviewCreateRequest;
import boostcampsnu.gwanakbnbserver.dto.review.ReviewCreateResponse;
import boostcampsnu.gwanakbnbserver.dto.review.ReviewUpdateRequest;
import boostcampsnu.gwanakbnbserver.dto.review.ReviewUpdateResponse;
import boostcampsnu.gwanakbnbserver.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "리뷰 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "리뷰 작성", security = @SecurityRequirement(name = "bearerAuth"))
    public ReviewCreateResponse create(@RequestBody @Valid ReviewCreateRequest request) {
        return reviewService.createReview(request);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "리뷰 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public ReviewUpdateResponse update(
            @PathVariable UUID reviewId,
            @RequestBody @Valid ReviewUpdateRequest request
    ) {
        return reviewService.updateReview(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "리뷰 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
    }
}
