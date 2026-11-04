package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoutePoiResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer total;

    @JsonProperty("route_length_km")
    private Double routeLengthKm;

    @JsonProperty("buffer_km")
    private Double bufferKm;

    /** false si OSRM no respondió y se usó línea recta entre waypoints. */
    private Boolean routed;

    @JsonProperty("route_geometry")
    private List<WaypointDTO> routeGeometry;

    private List<RoutePoiItemDTO> pois;

    @JsonProperty("by_type")
    private Map<String, Long> byType;
}

