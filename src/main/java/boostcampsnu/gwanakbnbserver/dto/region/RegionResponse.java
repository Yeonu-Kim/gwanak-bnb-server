package boostcampsnu.gwanakbnbserver.dto.region;

import boostcampsnu.gwanakbnbserver.domain.room.Region;

import java.util.UUID;

public record RegionResponse(
        UUID id,
        String name,
        Double latMin,
        Double latMax,
        Double lngMin,
        Double lngMax
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getName(),
                region.getLatMin(),
                region.getLatMax(),
                region.getLngMin(),
                region.getLngMax()
        );
    }
}
