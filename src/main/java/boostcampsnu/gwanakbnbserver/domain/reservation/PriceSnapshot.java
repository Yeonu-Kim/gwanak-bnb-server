package boostcampsnu.gwanakbnbserver.domain.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PriceSnapshot {

    @Column(name = "snapshot_base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "snapshot_nights")
    private int nights;

    @Column(name = "snapshot_discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;
}
