package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.room.Room;
import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RoomRepositoryTest {

    @Autowired RoomRepository roomRepository;
    @Autowired UserRepository userRepository;

    private User host;

    @BeforeEach
    void setUp() {
        host = userRepository.save(User.builder()
                .loginId("host@test.com").password("pw").name("호스트").userType(UserType.HOST).build());

        Room room1 = Room.builder()
                .name("숙소1").maxGuests(4)
                .pricePerNight(BigDecimal.valueOf(100000)).host(host).build();
        Room room2 = Room.builder()
                .name("숙소2").maxGuests(2)
                .pricePerNight(BigDecimal.valueOf(80000)).host(host).build();

        // averageScore 필드는 package-private이므로 save 후 직접 확인
        roomRepository.save(room1);
        roomRepository.save(room2);
    }

    @Test
    @DisplayName("호스트 ID로 숙소 수를 카운트한다")
    void countByHostId_returnsCorrectCount() {
        long count = roomRepository.countByHostId(host.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 호스트의 숙소는 카운트에 포함되지 않는다")
    void countByHostId_excludesOtherHostRooms() {
        User otherHost = userRepository.save(User.builder()
                .loginId("other@test.com").password("pw").name("다른호스트").userType(UserType.HOST).build());

        long count = roomRepository.countByHostId(otherHost.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("existsByIdAndHostLoginId: 본인 숙소이면 true")
    void existsByIdAndHostLoginId_whenOwner_returnsTrue() {
        Room room = roomRepository.findAll().get(0);

        boolean exists = roomRepository.existsByIdAndHostLoginId(room.getId(), "host@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByIdAndHostLoginId: 타인 숙소이면 false")
    void existsByIdAndHostLoginId_whenNotOwner_returnsFalse() {
        Room room = roomRepository.findAll().get(0);

        boolean exists = roomRepository.existsByIdAndHostLoginId(room.getId(), "other@test.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("숙소가 없는 호스트의 평균 점수는 0.0이다")
    void findAverageScoreByHostId_whenNoRooms_returnsZero() {
        User newHost = userRepository.save(User.builder()
                .loginId("new@test.com").password("pw").name("신규").userType(UserType.HOST).build());

        double avg = roomRepository.findAverageScoreByHostId(newHost.getId());

        assertThat(avg).isEqualTo(0.0);
    }
}
