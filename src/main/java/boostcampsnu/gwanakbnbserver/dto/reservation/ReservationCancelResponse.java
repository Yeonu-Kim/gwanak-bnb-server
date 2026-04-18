package boostcampsnu.gwanakbnbserver.dto.reservation;

import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;

import java.util.UUID;

public record ReservationCancelResponse(UUID id, ReservationStatus status) {
    public static ReservationCancelResponse from(Reservation r) {
        return new ReservationCancelResponse(r.getId(), r.getStatus());
    }
}
