package boostcampsnu.gwanakbnbserver.domain.room;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "room_discounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private int minNights;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Builder
    public RoomDiscount(Room room, int minNights, BigDecimal discountRate) {
        this.room = room;
        this.minNights = minNights;
        this.discountRate = discountRate;
    }
}
