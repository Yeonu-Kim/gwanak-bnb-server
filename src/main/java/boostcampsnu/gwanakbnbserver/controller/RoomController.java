package boostcampsnu.gwanakbnbserver.controller;

import boostcampsnu.gwanakbnbserver.dto.common.PageResponse;
import boostcampsnu.gwanakbnbserver.dto.review.RoomReviewsResponse;
import boostcampsnu.gwanakbnbserver.dto.room.*;
import boostcampsnu.gwanakbnbserver.service.ReviewService;
import boostcampsnu.gwanakbnbserver.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "숙소 CRUD 및 가격 조회 API")
public class RoomController {

    private final RoomService roomService;
    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "숙소 목록 조회",
            description = "keyword: 이름/설명 기반 시맨틱 검색 (MySQL FULLTEXT ngram). " +
                    "regionId: 해당 Region의 위/경도 바운딩박스 내 숙소 반환. " +
                    "checkIn+checkOut: 해당 기간에 예약 가능한 숙소만 반환.")
    public PageResponse<RoomSummaryResponse> getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return roomService.getRooms(keyword, categoryId, regionId, checkIn, checkOut, guests, minPrice, maxPrice, page, size);
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "숙소 상세 조회")
    public RoomDetailResponse getRoom(@PathVariable UUID roomId) {
        return roomService.getRoom(roomId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "숙소 등록", security = @SecurityRequirement(name = "bearerAuth"))
    public RoomCreateResponse createRoom(@RequestBody @Valid RoomCreateRequest request) {
        return roomService.createRoom(request);
    }

    @PutMapping("/{roomId}")
    @Operation(summary = "숙소 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public RoomUpdateResponse updateRoom(
            @PathVariable UUID roomId,
            @RequestBody @Valid RoomUpdateRequest request
    ) {
        return roomService.updateRoom(roomId, request);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "숙소 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public void deleteRoom(@PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
    }

    @GetMapping("/{roomId}/reviews")
    @Operation(summary = "숙소 리뷰 목록 조회")
    public RoomReviewsResponse getReviews(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reviewService.getRoomReviews(roomId, page, size);
    }

    @GetMapping("/{roomId}/prices")
    @Operation(summary = "날짜별 가격 조회")
    public RoomPricesResponse getPrices(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return roomService.getPrices(roomId, from, to);
    }
}
