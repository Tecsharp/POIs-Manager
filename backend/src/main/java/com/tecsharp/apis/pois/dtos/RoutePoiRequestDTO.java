package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePoiRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<WaypointDTO> waypoints;

    @JsonProperty("buffer_km")
    @Builder.Default
    private Double bufferKm = 8.0;

    private List<String> types;

    @JsonProperty("max_results")
    @Builder.Default
    private Integer maxResults = 50;

    @JsonProperty("include_closed")
    @Builder.Default
    private Boolean includeClosed = false;

    @JsonProperty("min_rating")
    private Double minRating;

    @JsonProperty("verified_only")
    @Builder.Default
    private Boolean verifiedOnly = false;
}

