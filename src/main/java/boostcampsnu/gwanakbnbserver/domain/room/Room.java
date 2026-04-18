package boostcampsnu.gwanakbnbserver.domain.room;

import boostcampsnu.gwanakbnbserver.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String shortDescription;

    @Column(nullable = false)
    private int maxGuests;

    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxInfants;
    private Integer maxPets;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(precision = 12, scale = 2)
    private BigDecimal weekendPricePerNight;

    @Column(length = 10)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    private Double latitude;
    private Double longitude;

    private String thumbnailUrl;

    @Column(nullable = false)
    private double averageScore = 0.0;

    @Column(nullable = false)
    private int reviewCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNum ASC")
    private List<RoomImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomDiscount> discounts = new ArrayList<>();

    @Builder
    public Room(String name, Category category, Region region, String shortDescription,
                String description, int maxGuests, Integer maxAdults, Integer maxChildren,
                Integer maxInfants, Integer maxPets, BigDecimal pricePerNight,
                BigDecimal weekendPricePerNight, String country, String city, String state,
                Double latitude, Double longitude, User host) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.shortDescription = shortDescription;
        this.description = description;
        this.maxGuests = maxGuests;
        this.maxAdults = maxAdults;
        this.maxChildren = maxChildren;
        this.maxInfants = maxInfants;
        this.maxPets = maxPets;
        this.pricePerNight = pricePerNight;
        this.weekendPricePerNight = weekendPricePerNight;
        this.country = country;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.host = host;
    }

    public void updateReviewStats(double averageScore, int reviewCount) {
        this.averageScore = averageScore;
        this.reviewCount = reviewCount;
    }

    public void update(String name, Category category, Region region, String shortDescription,
                       String description, Integer maxGuests, Integer maxAdults, Integer maxChildren,
                       Integer maxInfants, Integer maxPets, BigDecimal pricePerNight,
                       BigDecimal weekendPricePerNight, String country, String city, String state,
                       Double latitude, Double longitude) {
        if (name != null) this.name = name;
        if (category != null) this.category = category;
        if (region != null) this.region = region;
        if (shortDescription != null) this.shortDescription = shortDescription;
        if (description != null) this.description = description;
        if (maxGuests != null) this.maxGuests = maxGuests;
        if (maxAdults != null) this.maxAdults = maxAdults;
        if (maxChildren != null) this.maxChildren = maxChildren;
        if (maxInfants != null) this.maxInfants = maxInfants;
        if (maxPets != null) this.maxPets = maxPets;
        if (pricePerNight != null) this.pricePerNight = pricePerNight;
        if (weekendPricePerNight != null) this.weekendPricePerNight = weekendPricePerNight;
        if (country != null) this.country = country;
        if (city != null) this.city = city;
        if (state != null) this.state = state;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }
}
