package boostcampsnu.gwanakbnbserver.dto.room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RoomPricesResponse(List<DatePrice> prices) {

    public record DatePrice(LocalDate date, BigDecimal price) {
    }
}
