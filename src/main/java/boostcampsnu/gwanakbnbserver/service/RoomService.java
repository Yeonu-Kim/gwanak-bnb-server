package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.room.Category;
import boostcampsnu.gwanakbnbserver.domain.room.Region;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.common.PageResponse;
import boostcampsnu.gwanakbnbserver.dto.room.*;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.domain.reservation.ReservationStatus;
import boostcampsnu.gwanakbnbserver.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> getRooms(String keyword,
                                                       UUID categoryId, UUID regionId,
                                                       LocalDate checkIn, LocalDate checkOut,
                                                       Integer guests,
                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                       int page, int size) {
        if (checkIn != null && checkOut != null && !checkOut.isAfter(checkIn)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // FULLTEXT 검색: 관련도 순으로 최대 200개 ID 조회 후 Specification 필터에 전달
        List<UUID> keywordMatchedIds = null;
        if (keyword != null && !keyword.isBlank()) {
            keywordMatchedIds = roomRepository.findIdsByKeyword(keyword.trim())
                    .stream()
                    .map(RoomService::hexToUuid)
                    .toList();
        }

        int clampedSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, clampedSize);

        return PageResponse.from(
                roomRepository.findAll(
                        RoomSpecification.withFilters(
                                categoryId, regionId, guests, minPrice, maxPrice,
                                checkIn, checkOut, keywordMatchedIds),
                        pageable
                ).map(RoomSummaryResponse::from)
        );
    }

    /** BINARY(16)로 저장된 UUID의 HEX 문자열을 UUID로 변환한다. */
    private static UUID hexToUuid(String hex) {
        return UUID.fromString(
                hex.substring(0, 8) + "-" +
                hex.substring(8, 12) + "-" +
                hex.substring(12, 16) + "-" +
                hex.substring(16, 20) + "-" +
                hex.substring(20)
        );
    }

    @Transactional(readOnly = true)
    public RoomDetailResponse getRoom(UUID roomId) {
        Room room = roomRepository.findByIdWithImages(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        roomRepository.findByIdWithDiscounts(roomId);

        return RoomDetailResponse.from(room);
    }

    @Transactional
    public RoomCreateResponse createRoom(RoomCreateRequest request) {
        User host = getCurrentUser();
        if (host.getUserType() != UserType.HOST) {
            throw new AppException(ErrorCode.HOST_ONLY);
        }

        Category category = resolveCategory(request.categoryId());
        Region region = resolveRegion(request.regionId());

        Room room = Room.builder()
                .name(request.name())
                .category(category)
                .region(region)
                .shortDescription(request.shortDescription())
                .description(request.description())
                .maxGuests(request.maxGuests())
                .maxAdults(request.maxAdults())
                .maxChildren(request.maxChildren())
                .maxInfants(request.maxInfants())
                .maxPets(request.maxPets())
                .pricePerNight(request.pricePerNight())
                .weekendPricePerNight(request.weekendPricePerNight())
                .country(request.country())
                .city(request.city())
                .state(request.state())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .host(host)
                .build();

        return RoomCreateResponse.from(roomRepository.save(room));
    }

    @Transactional
    public RoomUpdateResponse updateRoom(UUID roomId, RoomUpdateRequest request) {
        Room room = findRoomOrThrow(roomId);
        assertOwner(room);

        Category category = resolveCategory(request.categoryId());
        Region region = resolveRegion(request.regionId());

        room.update(
                request.name(), category, region,
                request.shortDescription(), request.description(),
                request.maxGuests(), request.maxAdults(), request.maxChildren(),
                request.maxInfants(), request.maxPets(),
                request.pricePerNight(), request.weekendPricePerNight(),
                request.country(), request.city(), request.state(),
                request.latitude(), request.longitude()
        );

        return RoomUpdateResponse.from(room);
    }

    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = findRoomOrThrow(roomId);
        assertOwner(room);
        if (reservationRepository.existsByRoomIdAndStatusIn(
                roomId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))) {
            throw new AppException(ErrorCode.HAS_ACTIVE_RESERVATION);
        }
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public RoomPricesResponse getPrices(UUID roomId, LocalDate from, LocalDate to) {
        if (!to.isAfter(from)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Room room = findRoomOrThrow(roomId);
        List<RoomPricesResponse.DatePrice> prices = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            BigDecimal price = (isWeekend && room.getWeekendPricePerNight() != null)
                    ? room.getWeekendPricePerNight()
                    : room.getPricePerNight();
            prices.add(new RoomPricesResponse.DatePrice(date, price));
        }

        return new RoomPricesResponse(prices);
    }

    private Room findRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void assertOwner(Room room) {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!room.getHost().getLoginId().equals(loginId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private User getCurrentUser() {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Region resolveRegion(UUID regionId) {
        if (regionId == null) return null;
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new AppException(ErrorCode.REGION_NOT_FOUND));
    }
}
