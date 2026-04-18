package boostcampsnu.gwanakbnbserver.controller;

import boostcampsnu.gwanakbnbserver.dto.host.HostProfileResponse;
import boostcampsnu.gwanakbnbserver.dto.host.HostRegisterRequest;
import boostcampsnu.gwanakbnbserver.dto.host.HostRegisterResponse;
import boostcampsnu.gwanakbnbserver.service.HostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hosts")
@RequiredArgsConstructor
@Tag(name = "Hosts", description = "호스트 프로필 및 전환 API")
public class HostController {

    private final HostService hostService;

    @GetMapping("/{hostId}")
    @Operation(summary = "호스트 프로필 조회")
    public HostProfileResponse getProfile(@PathVariable UUID hostId) {
        return hostService.getHostProfile(hostId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "호스트 전환 신청", security = @SecurityRequirement(name = "bearerAuth"))
    public HostRegisterResponse register(@RequestBody @Valid HostRegisterRequest request) {
        return hostService.registerHost(request);
    }
}
