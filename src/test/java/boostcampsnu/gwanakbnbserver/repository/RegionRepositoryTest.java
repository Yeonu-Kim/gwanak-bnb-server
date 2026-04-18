package boostcampsnu.gwanakbnbserver.repository;

import boostcampsnu.gwanakbnbserver.domain.room.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RegionRepositoryTest {

    @Autowired
    private RegionRepository regionRepository;

    @Test
    @DisplayName("저장한 지역을 findAll로 조회할 수 있다")
    void saveAndFindAll_returnsSavedRegions() {
        regionRepository.save(Region.builder()
                .name("제주도").latMin(33.0).latMax(33.9).lngMin(126.1).lngMax(126.9).build());
        regionRepository.save(Region.builder()
                .name("서울").latMin(37.4).latMax(37.7).lngMin(126.8).lngMax(127.2).build());

        List<Region> regions = regionRepository.findAll();

        assertThat(regions).hasSize(2);
        assertThat(regions).extracting(Region::getName).containsExactlyInAnyOrder("제주도", "서울");
    }

    @Test
    @DisplayName("지역 좌표 정보가 정확히 저장된다")
    void save_persistsCoordinates() {
        regionRepository.save(Region.builder()
                .name("제주도").latMin(33.1).latMax(33.9).lngMin(126.2).lngMax(126.9).build());

        Region found = regionRepository.findAll().get(0);

        assertThat(found.getLatMin()).isEqualTo(33.1);
        assertThat(found.getLatMax()).isEqualTo(33.9);
        assertThat(found.getLngMin()).isEqualTo(126.2);
        assertThat(found.getLngMax()).isEqualTo(126.9);
    }
}
