package boostcampsnu.gwanakbnbserver.dto.reservation;

import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationSummaryResponse(
        UUID id,
        ReservationRoomInfo room,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        BigDecimal totalPrice,
        ReservationStatus status
) {
    public static ReservationSummaryResponse from(Reservation r) {
        return new ReservationSummaryResponse(
                r.getId(),
                ReservationRoomInfo.from(r.getRoom()),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getGuestCount(),
                r.getTotalPrice(),
                r.getStatus()
        );
    }
}
