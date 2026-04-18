package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .loginId("user@test.com")
                .password("encoded-password")
                .name("테스트유저")
                .userType(UserType.GUEST)
                .build());
    }

    @Test
    @DisplayName("존재하는 loginId로 조회하면 User를 반환한다")
    void findByLoginId_whenExists_returnsUser() {
        Optional<User> result = userRepository.findByLoginId("user@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("테스트유저");
        assertThat(result.get().getUserType()).isEqualTo(UserType.GUEST);
    }

    @Test
    @DisplayName("존재하지 않는 loginId로 조회하면 빈 Optional을 반환한다")
    void findByLoginId_whenNotExists_returnsEmpty() {
        Optional<User> result = userRepository.findByLoginId("nobody@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하는 loginId는 existsByLoginId가 true를 반환한다")
    void existsByLoginId_whenExists_returnsTrue() {
        boolean exists = userRepository.existsByLoginId("user@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 loginId는 existsByLoginId가 false를 반환한다")
    void existsByLoginId_whenNotExists_returnsFalse() {
        boolean exists = userRepository.existsByLoginId("nobody@test.com");

        assertThat(exists).isFalse();
    }
}
