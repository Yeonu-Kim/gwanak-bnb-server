package boostcampsnu.gwanakbnbserver.domain.review;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "review_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String url;

    @Column(length = 255)
    private String caption;

    @Column(nullable = false)
    private int orderNum;

    @Builder
    public ReviewImage(Review review, String url, String caption, int orderNum) {
        this.review = review;
        this.url = url;
        this.caption = caption;
        this.orderNum = orderNum;
    }
}
