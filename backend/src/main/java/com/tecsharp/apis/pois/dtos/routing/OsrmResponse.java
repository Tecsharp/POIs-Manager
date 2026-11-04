package com.tecsharp.apis.pois.dtos.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Subconjunto de la respuesta de OSRM (http://project-osrm.org) que usamos.
 * El RestClient de este servicio usa un ObjectMapper propio (no el de Spring Boot), que por
 * defecto SÍ falla ante propiedades desconocidas; OSRM manda varias que no nos interesan
 * (legs, weight, weight_name, summary...), de ahí el ignoreUnknown en cada nivel.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmResponse(String code, List<OsrmRoute> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmRoute(OsrmGeometry geometry, Double distance, Double duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OsrmGeometry(List<List<Double>> coordinates) {
    }
}
