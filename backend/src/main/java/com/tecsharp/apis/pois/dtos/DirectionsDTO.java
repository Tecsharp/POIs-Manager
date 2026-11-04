package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DirectionsDTO(boolean routed,
                            @JsonProperty("distance_km") double distanceKm,
                            @JsonProperty("duration_min") Double durationMin,
                            List<WaypointDTO> geometry) {
}
