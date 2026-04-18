package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.room.Region;
import boostcampsnu.gwanakbnbserver.dto.region.RegionResponse;
import boostcampsnu.gwanakbnbserver.repository.RegionRepository;
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
class RegionServiceTest {

    @Mock RegionRepository regionRepository;

    @InjectMocks RegionService regionService;

    @Test
    @DisplayName("지역 목록 조회 성공 - 전체 지역을 반환한다")
    void getAll_success() {
        Region r1 = Region.builder().name("서울").build();
        Region r2 = Region.builder().name("부산").build();
        ReflectionTestUtils.setField(r1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(r2, "id", UUID.randomUUID());

        given(regionRepository.findAll()).willReturn(List.of(r1, r2));

        List<RegionResponse> result = regionService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("서울");
        assertThat(result.get(1).name()).isEqualTo("부산");
    }

    @Test
    @DisplayName("지역이 없으면 빈 목록을 반환한다")
    void getAll_empty_returnsEmptyList() {
        given(regionRepository.findAll()).willReturn(List.of());

        List<RegionResponse> result = regionService.getAll();

        assertThat(result).isEmpty();
    }
}
