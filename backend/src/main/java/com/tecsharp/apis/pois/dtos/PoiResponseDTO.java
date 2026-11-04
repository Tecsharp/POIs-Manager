package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tecsharp.apis.pois.models.BusinessHours;
import com.tecsharp.apis.pois.models.Poi;
import com.tecsharp.apis.pois.models.enums.PoiType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PoiResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
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

    @JsonProperty("accepts_cards")
    private Boolean acceptsCards;

    private Double rating;
    private Boolean verified;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Convierte una entidad Poi a PoiResponseDTO
     */
    public static PoiResponseDTO fromEntity(Poi poi) {
        return PoiResponseDTO.builder()
                .id(poi.getId())
                .name(poi.getName())
                .type(poi.getType())
                .lat(poi.getLat())
                .lng(poi.getLng())
                .address(poi.getAddress())
                .phone(poi.getPhone())
                .website(poi.getWebsite())
                .hours(poi.getHours())
                .specialties(poi.getSpecialties())
                .description(poi.getDescription())
                .acceptsCards(poi.getAcceptsCards())
                .rating(poi.getRating())
                .verified(poi.getVerified())
                .createdAt(poi.getCreatedAt())
                .updatedAt(poi.getUpdatedAt())
                .build();
    }
}

