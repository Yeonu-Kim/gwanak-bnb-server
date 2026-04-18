package boostcampsnu.gwanakbnbserver.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;

    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private HostType hostType;

    private Instant hostStartedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public User(String loginId, String password, String name, UserType userType) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.userType = userType != null ? userType : UserType.GUEST;
        this.createdAt = Instant.now();
    }

    public void becomeHost(String name, String description, String thumbnailUrl) {
        if (name != null) this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.userType = UserType.HOST;
        this.hostType = HostType.HOST;
        this.hostStartedAt = Instant.now();
    }
}
