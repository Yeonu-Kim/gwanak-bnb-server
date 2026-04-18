package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.room.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("저장한 카테고리를 findAll로 조회할 수 있다")
    void saveAndFindAll_returnsSavedCategories() {
        categoryRepository.save(Category.builder().name("한옥").iconUrl("http://icon1.com").build());
        categoryRepository.save(Category.builder().name("아파트").iconUrl("http://icon2.com").build());

        List<Category> categories = categoryRepository.findAll();

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder("한옥", "아파트");
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 리스트를 반환한다")
    void findAll_whenEmpty_returnsEmptyList() {
        List<Category> categories = categoryRepository.findAll();

        assertThat(categories).isEmpty();
    }
}
