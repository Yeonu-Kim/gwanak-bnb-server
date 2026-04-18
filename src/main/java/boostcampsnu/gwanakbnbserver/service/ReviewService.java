package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.review.Review;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.dto.review.*;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.ReservationRepository;
import boostcampsnu.gwanakbnbserver.repository.ReviewRepository;
import boostcampsnu.gwanakbnbserver.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public ReviewCreateResponse createReview(ReviewCreateRequest request) {
        Reservation reservation = reservationRepository.findByIdWithRoomAndUser(request.reservationId())
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

        String loginId = currentLoginId();
        if (!reservation.getUser().getLoginId().equals(loginId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (!LocalDate.now().isAfter(reservation.getCheckOut())) {
            throw new AppException(ErrorCode.CHECKOUT_REQUIRED);
        }
        if (reviewRepository.existsByReservationId(request.reservationId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .reservation(reservation)
                .room(reservation.getRoom())
                .reviewer(reservation.getUser())
                .score(request.score())
                .content(request.content())
                .build();

        Review saved = reviewRepository.save(review);
        updateRoomStats(reservation.getRoom());
        return ReviewCreateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RoomReviewsResponse getRoomReviews(UUID roomId, int page, int size) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        Page<Review> reviewPage = reviewRepository.findByRoomId(roomId, PageRequest.of(page, size));
        Double avg = reviewRepository.findAverageScoreByRoomId(roomId);

        return new RoomReviewsResponse(
                avg != null ? avg : 0.0,
                reviewPage.getContent().stream().map(ReviewResponse::from).toList(),
                reviewPage.getTotalElements()
        );
    }

    @Transactional
    public ReviewUpdateResponse updateReview(UUID reviewId, ReviewUpdateRequest request) {
        Review review = findReviewOrThrow(reviewId);
        assertReviewer(review);
        review.update(request.score(), request.content());
        updateRoomStats(review.getRoom());
        return ReviewUpdateResponse.from(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        Review review = findReviewOrThrow(reviewId);
        assertReviewer(review);
        Room room = review.getRoom();
        reviewRepository.delete(review);
        updateRoomStats(room);
    }

    private void updateRoomStats(Room room) {
        Double avg = reviewRepository.findAverageScoreByRoomId(room.getId());
        int count = reviewRepository.countByRoomId(room.getId());
        room.updateReviewStats(avg != null ? avg : 0.0, count);
    }

    private Review findReviewOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void assertReviewer(Review review) {
        if (!review.getReviewer().getLoginId().equals(currentLoginId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private String currentLoginId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
