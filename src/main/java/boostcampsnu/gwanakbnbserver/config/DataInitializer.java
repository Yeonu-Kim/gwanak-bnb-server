package boostcampsnu.gwanakbnbserver.config;

import boostcampsnu.gwanakbnbserver.domain.reservation.PriceSnapshot;
import boostcampsnu.gwanakbnbserver.domain.reservation.Reservation;
import boostcampsnu.gwanakbnbserver.domain.review.Review;
import boostcampsnu.gwanakbnbserver.domain.review.ReviewImage;
import boostcampsnu.gwanakbnbserver.domain.room.Category;
import boostcampsnu.gwanakbnbserver.domain.room.Region;
import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.room.RoomImage;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.repository.*;
import tools.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Getter @Setter @NoArgsConstructor
    static class DummyData {
        private List<CategoryData> categories;
        private List<RegionData> regions;
        private List<UserData> hosts;
        private List<UserData> guests;
        private List<RoomData> rooms;
        private List<ReservationData> reservations;
        private List<ReviewData> reviews;
    }

    @Getter @Setter @NoArgsConstructor
    static class CategoryData {
        private String name;
        private String iconUrl;
    }

    @Getter @Setter @NoArgsConstructor
    static class RegionData {
        private String name;
        private double latMin, latMax, lngMin, lngMax;
    }

    @Getter @Setter @NoArgsConstructor
    static class UserData {
        private String loginId;
        private String name;
    }

    @Getter @Setter @NoArgsConstructor
    static class RoomData {
        private String name;
        private String shortDescription;
        private String description;
        private String categoryName;
        private String regionName;
        private String hostLoginId;
        private int maxGuests;
        private Integer maxAdults, maxChildren, maxInfants, maxPets;
        private BigDecimal pricePerNight;
        private BigDecimal weekendPricePerNight;
        private String country, city, state;
        private double latitude, longitude;
        private List<ImageData> images;
    }

    @Getter @Setter @NoArgsConstructor
    static class ImageData {
        private String url;
        private String caption;
    }

    @Getter @Setter @NoArgsConstructor
    static class ReservationData {
        private int roomIndex;
        private String userLoginId;
        private String checkIn;
        private String checkOut;
        private int guestCount;
        private BigDecimal basePrice;
        private int nights;
    }

    @Getter @Setter @NoArgsConstructor
    static class ReviewData {
        private int reservationIndex;
        private int score;
        private String content;
        private List<String> imageUrls;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (categoryRepository.count() > 0) return;

        DummyData data = objectMapper.readValue(
                new ClassPathResource("db/dummy-data.json").getInputStream(),
                DummyData.class
        );

        Map<String, Category> categoryMap = new HashMap<>();
        for (CategoryData c : data.getCategories()) {
            Category category = categoryRepository.save(
                    Category.builder().name(c.getName()).iconUrl(c.getIconUrl()).build());
            categoryMap.put(c.getName(), category);
        }

        Map<String, Region> regionMap = new HashMap<>();
        for (RegionData r : data.getRegions()) {
            Region region = regionRepository.save(Region.builder()
                    .name(r.getName())
                    .latMin(r.getLatMin()).latMax(r.getLatMax())
                    .lngMin(r.getLngMin()).lngMax(r.getLngMax()).build());
            regionMap.put(r.getName(), region);
        }

        String encoded = passwordEncoder.encode("password123");
        Map<String, User> userMap = new HashMap<>();

        for (UserData h : data.getHosts()) {
            User host = userRepository.save(User.builder()
                    .loginId(h.getLoginId()).password(encoded)
                    .name(h.getName()).userType(UserType.HOST).build());
            host.becomeHost(h.getName(), null, null);
            userMap.put(h.getLoginId(), host);
        }

        for (UserData g : data.getGuests()) {
            User guest = userRepository.save(User.builder()
                    .loginId(g.getLoginId()).password(encoded)
                    .name(g.getName()).userType(UserType.GUEST).build());
            userMap.put(g.getLoginId(), guest);
        }

        List<Room> rooms = new ArrayList<>();
        for (RoomData rd : data.getRooms()) {
            Room room = roomRepository.save(Room.builder()
                    .name(rd.getName())
                    .shortDescription(rd.getShortDescription())
                    .description(rd.getDescription())
                    .category(categoryMap.get(rd.getCategoryName()))
                    .region(regionMap.get(rd.getRegionName()))
                    .host(userMap.get(rd.getHostLoginId()))
                    .maxGuests(rd.getMaxGuests())
                    .maxAdults(rd.getMaxAdults())
                    .maxChildren(rd.getMaxChildren())
                    .maxInfants(rd.getMaxInfants())
                    .maxPets(rd.getMaxPets())
                    .pricePerNight(rd.getPricePerNight())
                    .weekendPricePerNight(rd.getWeekendPricePerNight())
                    .country(rd.getCountry())
                    .city(rd.getCity())
                    .state(rd.getState())
                    .latitude(rd.getLatitude())
                    .longitude(rd.getLongitude())
                    .build());

            if (rd.getImages() != null) {
                for (int i = 0; i < rd.getImages().size(); i++) {
                    ImageData img = rd.getImages().get(i);
                    room.getImages().add(RoomImage.builder()
                            .room(room).url(img.getUrl()).caption(img.getCaption()).orderNum(i + 1).build());
                }
            }
            rooms.add(room);
        }

        List<Reservation> reservations = new ArrayList<>();
        for (ReservationData rd : data.getReservations()) {
            Room room = rooms.get(rd.getRoomIndex());
            User user = userMap.get(rd.getUserLoginId());
            BigDecimal total = rd.getBasePrice().multiply(BigDecimal.valueOf(rd.getNights()));
            PriceSnapshot snapshot = new PriceSnapshot(rd.getBasePrice(), rd.getNights(), null);
            Reservation reservation = reservationRepository.save(Reservation.builder()
                    .room(room).user(user)
                    .checkIn(LocalDate.parse(rd.getCheckIn()))
                    .checkOut(LocalDate.parse(rd.getCheckOut()))
                    .guestCount(rd.getGuestCount())
                    .totalPrice(total)
                    .priceSnapshot(snapshot)
                    .build());
            reservations.add(reservation);
        }

        for (ReviewData rd : data.getReviews()) {
            Reservation reservation = reservations.get(rd.getReservationIndex());
            Review review = reviewRepository.save(Review.builder()
                    .reservation(reservation)
                    .room(reservation.getRoom())
                    .reviewer(reservation.getUser())
                    .score(rd.getScore())
                    .content(rd.getContent())
                    .build());

            if (rd.getImageUrls() != null) {
                for (int i = 0; i < rd.getImageUrls().size(); i++) {
                    review.getImages().add(ReviewImage.builder()
                            .review(review).url(rd.getImageUrls().get(i)).orderNum(i + 1).build());
                }
            }
        }

        for (Room room : rooms) {
            Double avg = reviewRepository.findAverageScoreByRoomId(room.getId());
            int count = reviewRepository.countByRoomId(room.getId());
            room.updateReviewStats(avg != null ? avg : 0.0, count);
        }
    }
}
