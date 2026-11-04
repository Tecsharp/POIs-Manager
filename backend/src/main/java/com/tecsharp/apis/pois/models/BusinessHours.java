package com.tecsharp.apis.pois.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Modelo para representar el horario de un POI de forma simplificada.
 * En lugar de definir cada día, se define un horario general y los días de descanso.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessHours implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("open_time")
    private String openTime;

    @JsonProperty("close_time")
    private String closeTime;

    /**
     * Lista de días de descanso (ej: ["sunday", "monday"])
     * Valores válidos: monday, tuesday, wednesday, thursday, friday, saturday, sunday
     */
    @JsonProperty("closed_days")
    private List<String> closedDays;

    /**
     * Comentarios adicionales sobre el horario
     * (ej: "Viernes cierra a las 23:00", "Horario reducido en días festivos")
     */
    private String comments;
}

