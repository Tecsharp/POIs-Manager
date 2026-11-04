package com.tecsharp.apis.pois.models;

import com.tecsharp.apis.pois.models.enums.PoiType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pois")
public class Poi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoiType type;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    private String address;

    private String phone;

    private String website;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = com.tecsharp.apis.pois.models.converters.BusinessHoursConverter.class)
    private BusinessHours hours;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = com.tecsharp.apis.pois.models.converters.ListToJsonConverter.class)
    private List<String> specialties;

    private String description;

    @Column(name = "accepts_cards")
    private Boolean acceptsCards;

    private Double rating;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
