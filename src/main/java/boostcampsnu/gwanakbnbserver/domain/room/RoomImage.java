package boostcampsnu.gwanakbnbserver.domain.room;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "room_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String url;

    @Column(length = 255)
    private String caption;

    @Column(nullable = false)
    private int orderNum;

    @Builder
    public RoomImage(Room room, String url, String caption, int orderNum) {
        this.room = room;
        this.url = url;
        this.caption = caption;
        this.orderNum = orderNum;
    }
}
