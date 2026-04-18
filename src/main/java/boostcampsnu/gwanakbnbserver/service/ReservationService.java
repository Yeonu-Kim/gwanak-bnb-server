package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;
import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.room.RoomDiscount;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.dto.common.PageResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCancelResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCreateRequest;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCreateResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationSummaryResponse;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.ReservationRepository;
import boostcampsnu.gwanakbnbserver.repository.RoomRepository;
import boostcampsnu.gwanakbnbserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReservationCreateResponse create(ReservationCreateRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (request.guestCount() > room.getMaxGuests()) {
            throw new AppException(ErrorCode.GUEST_LIMIT_EXCEEDED);
        }

        long overlapping = reservationRepository.countOverlapping(
                room.getId(), ReservationStatus.CANCELLED,
                request.checkIn(), request.checkOut()
        );
        if (overlapping > 0) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
        }

        User user = getCurrentUser();
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        BigDecimal basePrice = room.getPricePerNight();
        BigDecimal discountRate = room.getDiscounts().stream()
                .filter(d -> d.getMinNights() <= nights)
                .max(Comparator.comparing(RoomDiscount::getMinNights))
                .map(RoomDiscount::getDiscountRate)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalPrice = basePrice
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.ONE.subtract(discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation reservation = Reservation.builder()
                .room(room)
                .user(user)
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .guestCount(request.guestCount())
                .totalPrice(totalPrice)
                .priceSnapshot(new PriceSnapshot(basePrice, (int) nights, discountRate))
                .build();

        return ReservationCreateResponse.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationSummaryResponse> getMyReservations(ReservationStatus status, int page, int size) {
        String loginId = currentLoginId();
        var pageable = PageRequest.of(page, size);
        var result = (status != null)
                ? reservationRepository.findByUserLoginIdAndStatus(loginId, status, pageable)
                : reservationRepository.findByUserLoginId(loginId, pageable);
        return PageResponse.from(result.map(ReservationSummaryResponse::from));
    }

    @Transactional
    public ReservationCancelResponse cancel(UUID reservationId) {
        Reservation reservation = reservationRepository.findByIdWithRoomAndUser(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getUser().getLoginId().equals(currentLoginId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new AppException(ErrorCode.ALREADY_CANCELLED);
        }
        if (!reservation.getCheckIn().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.CANCEL_NOT_ALLOWED);
        }

        reservation.cancel();
        return ReservationCancelResponse.from(reservation);
    }

    private User getCurrentUser() {
        return userRepository.findByLoginId(currentLoginId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    }

    private String currentLoginId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
