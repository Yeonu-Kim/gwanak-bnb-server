package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.dto.region.RegionResponse;
import boostcampsnu.gwanakbnbserver.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> getAll() {
        return regionRepository.findAll().stream()
                .map(RegionResponse::from)
                .toList();
    }
}
