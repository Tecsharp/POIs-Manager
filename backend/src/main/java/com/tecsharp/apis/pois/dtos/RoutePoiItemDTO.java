package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoutePoiItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
    private String type;
    private Double lat;
    private Double lng;
    private String address;
    private Double rating;
    private Boolean verified;

    @JsonProperty("distance_km")
    private Double distanceKm;

    private Long upvotes;
    private Long downvotes;

    @JsonProperty("my_vote")
    private Integer myVote;

    @JsonProperty("created_by")
    private String createdBy;
}

