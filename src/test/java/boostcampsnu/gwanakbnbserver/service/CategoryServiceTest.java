package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.room.Category;
import boostcampsnu.gwanakbnbserver.dto.room.CategoryResponse;
import boostcampsnu.gwanakbnbserver.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;

    @InjectMocks CategoryService categoryService;

    @Test
    @DisplayName("카테고리 목록 조회 성공 - 전체 카테고리를 반환한다")
    void getAll_success() {
        Category c1 = Category.builder().name("산").iconUrl("mountain.png").build();
        Category c2 = Category.builder().name("바다").iconUrl("ocean.png").build();
        ReflectionTestUtils.setField(c1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(c2, "id", UUID.randomUUID());

        given(categoryRepository.findAll()).willReturn(List.of(c1, c2));

        List<CategoryResponse> result = categoryService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("산");
        assertThat(result.get(1).name()).isEqualTo("바다");
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
    void getAll_empty_returnsEmptyList() {
        given(categoryRepository.findAll()).willReturn(List.of());

        List<CategoryResponse> result = categoryService.getAll();

        assertThat(result).isEmpty();
    }
}
