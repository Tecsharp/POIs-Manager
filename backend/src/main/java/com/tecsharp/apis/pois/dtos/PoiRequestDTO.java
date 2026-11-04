package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tecsharp.apis.pois.models.BusinessHours;
import com.tecsharp.apis.pois.models.enums.PoiType;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PoiRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private PoiType type;
    private Double lat;
    private Double lng;
    private String address;
    private String phone;
    private String website;
    private BusinessHours hours;
    private List<String> specialties;
    private String description;

    private Double rating;

    @JsonProperty("accepts_cards")
    private Boolean acceptsCards;
}
