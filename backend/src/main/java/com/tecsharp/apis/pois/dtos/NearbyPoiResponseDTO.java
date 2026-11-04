package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record NearbyPoiResponseDTO(Integer total,
                                   Double lat,
                                   Double lng,
                                   @JsonProperty("radius_km") Double radiusKm,
                                   List<RoutePoiItemDTO> pois,
                                   @JsonProperty("by_type") Map<String, Long> byType) implements Serializable {
}
