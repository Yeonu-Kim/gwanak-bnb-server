package boostcampsnu.gwanakbnbserver.dto.reservation;

import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationCreateResponse(
        UUID id,
        UUID roomId,
        UUID userId,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        BigDecimal totalPrice,
        PriceSnapshotResponse priceSnapshot,
        ReservationStatus status,
        Instant createdAt
) {
    public static ReservationCreateResponse from(Reservation r) {
        return new ReservationCreateResponse(
                r.getId(),
                r.getRoom().getId(),
                r.getUser().getId(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getGuestCount(),
                r.getTotalPrice(),
                PriceSnapshotResponse.from(r.getPriceSnapshot()),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
