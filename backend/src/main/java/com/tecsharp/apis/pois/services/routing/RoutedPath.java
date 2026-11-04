package com.tecsharp.apis.pois.services.routing;

import java.util.List;

/**
 * Resultado de intentar enrutar por carretera. Si OSRM no responde (caído, timeout, sin ruta),
 * routed=false y geometry son los waypoints originales unidos en línea recta: el corredor
 * de búsqueda sigue funcionando, solo pierde precisión.
 */
public record RoutedPath(boolean routed, double distanceKm, Double durationMin, List<double[]> geometry) {
}
