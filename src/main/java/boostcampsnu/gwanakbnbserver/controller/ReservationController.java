package boostcampsnu.gwanakbnbserver.controller;

import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.dto.common.PageResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCancelResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCreateRequest;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationCreateResponse;
import boostcampsnu.gwanakbnbserver.dto.reservation.ReservationSummaryResponse;
import boostcampsnu.gwanakbnbserver.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "예약 API")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "예약 생성", security = @SecurityRequirement(name = "bearerAuth"))
    public ReservationCreateResponse create(@RequestBody @Valid ReservationCreateRequest request) {
        return reservationService.create(request);
    }

    @GetMapping
    @Operation(summary = "내 예약 목록", security = @SecurityRequirement(name = "bearerAuth"))
    public PageResponse<ReservationSummaryResponse> getMyReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reservationService.getMyReservations(status, page, size);
    }

    @PatchMapping("/{reservationId}/cancel")
    @Operation(summary = "예약 취소", security = @SecurityRequirement(name = "bearerAuth"))
    public ReservationCancelResponse cancel(@PathVariable UUID reservationId) {
        return reservationService.cancel(reservationId);
    }
}
