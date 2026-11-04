package com.tecsharp.apis.pois.services.routing;

import com.tecsharp.apis.pois.dtos.WaypointDTO;

import java.util.List;

public interface RoutingService {

    /** Calcula la ruta real por carretera entre los waypoints dados, en orden. */
    RoutedPath route(List<WaypointDTO> waypoints);
}
