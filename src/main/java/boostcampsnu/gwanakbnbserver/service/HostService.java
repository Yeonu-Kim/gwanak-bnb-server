package boostcampsnu.gwanakbnbserver.service;

import boostcampsnu.gwanakbnbserver.domain.user.User;
import boostcampsnu.gwanakbnbserver.domain.user.UserType;
import boostcampsnu.gwanakbnbserver.dto.host.HostProfileResponse;
import boostcampsnu.gwanakbnbserver.dto.host.HostRegisterRequest;
import boostcampsnu.gwanakbnbserver.dto.host.HostRegisterResponse;
import boostcampsnu.gwanakbnbserver.exception.AppException;
import boostcampsnu.gwanakbnbserver.exception.ErrorCode;
import boostcampsnu.gwanakbnbserver.repository.RoomRepository;
import boostcampsnu.gwanakbnbserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HostService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public HostProfileResponse getHostProfile(UUID hostId) {
        User host = userRepository.findById(hostId)
                .filter(u -> u.getUserType() == UserType.HOST)
                .orElseThrow(() -> new AppException(ErrorCode.HOST_NOT_FOUND));

        long roomCount = roomRepository.countByHostId(hostId);
        double avgScore = roomRepository.findAverageScoreByHostId(hostId);
        return HostProfileResponse.of(host, roomCount, avgScore);
    }

    @Transactional
    public HostRegisterResponse registerHost(HostRegisterRequest request) {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getUserType() == UserType.HOST) {
            throw new AppException(ErrorCode.ALREADY_HOST);
        }

        user.becomeHost(request.name(), request.description(), request.thumbnailUrl());
        return HostRegisterResponse.from(user);
    }
}
