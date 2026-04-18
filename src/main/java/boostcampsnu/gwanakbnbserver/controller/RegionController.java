package boostcampsnu.gwanakbnbserver.controller;

import boostcampsnu.gwanakbnbserver.dto.region.RegionResponse;
import boostcampsnu.gwanakbnbserver.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Tag(name = "Regions", description = "지역 목록 API")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    @Operation(summary = "지역 목록 조회")
    public List<RegionResponse> getAll() {
        return regionService.getAll();
    }
}
