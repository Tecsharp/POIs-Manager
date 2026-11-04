package com.tecsharp.apis.pois.services.routing.impl;

import com.tecsharp.apis.pois.dtos.WaypointDTO;
import com.tecsharp.apis.pois.dtos.routing.OsrmResponse;
import com.tecsharp.apis.pois.services.routing.RoutedPath;
import com.tecsharp.apis.pois.services.routing.RoutingService;
import com.tecsharp.apis.pois.utils.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enruta por carretera usando OSRM (motor de ruteo open-source). Por defecto apunta al servidor
 * demo público router.project-osrm.org — gratis, sin API key, pero sin garantía de servicio.
 * shortcut: sin SLA ni control de disponibilidad propios; si el volumen crece, levantar un
 * contenedor OSRM propio con un extracto de México y apuntar pois.routing.osrm-url ahí.
 * Si falla o no responde a tiempo, se cae a línea recta entre waypoints (comportamiento previo).
 */
@Slf4j
@Service
public class RoutingServiceImpl implements RoutingService {

    private static final int MAX_QUERY_WAYPOINTS = 25;
    private static final int MAX_GEOMETRY_POINTS = 300;

    private final RestClient restClient;
    private final boolean enabled;

    public RoutingServiceImpl(@Value("${pois.routing.osrm-url:https://router.project-osrm.org}") String osrmUrl,
                              @Value("${pois.routing.enabled:true}") boolean enabled,
                              @Value("${pois.routing.timeout-ms:4000}") int timeoutMs) {
        this.enabled = enabled && osrmUrl != null && !osrmUrl.isBlank();
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build());
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(osrmUrl).requestFactory(factory).build();
        log.info("Ruteo por carretera {} (OSRM: {})", this.enabled ? "habilitado" : "deshabilitado", osrmUrl);
    }

    @Override
    public RoutedPath route(List<WaypointDTO> waypoints) {
        double[][] straight = waypoints.stream().map(w -> new double[]{w.getLat(), w.getLng()}).toArray(double[][]::new);
        if (!enabled || waypoints.size() < 2) {
            return fallback(straight);
        }
        try {
            String coords = downsample(waypoints, MAX_QUERY_WAYPOINTS).stream()
                    .map(w -> w.getLng() + "," + w.getLat())
                    .collect(Collectors.joining(";"));

            OsrmResponse response = restClient.get()
                    .uri(b -> b.path("/route/v1/driving/{coords}").queryParam("overview", "full").queryParam("geometries", "geojson").build(coords))
                    // El HttpClient del JDK anuncia Accept-Encoding: gzip pero no descomprime la respuesta él
                    // mismo; OSRM sí comprime si se lo pedimos, así que forzamos "identity" para evitarlo.
                    .header("Accept-Encoding", "identity")
                    .retrieve()
                    .body(OsrmResponse.class);

            if (response == null || !"Ok".equalsIgnoreCase(response.code()) || response.routes() == null || response.routes().isEmpty()) {
                log.warn("OSRM sin ruta válida (code={}), se usa línea recta", response != null ? response.code() : "null");
                return fallback(straight);
            }

            OsrmResponse.OsrmRoute route = response.routes().getFirst();
            List<double[]> geometry = downsample(route.geometry().coordinates().stream()
                    .map(c -> new double[]{c.get(1), c.get(0)}) // OSRM da [lng, lat]; nosotros usamos [lat, lng]
                    .toList(), MAX_GEOMETRY_POINTS);

            double distanceKm = route.distance() != null ? route.distance() / 1000.0 : GeoUtils.routeLength(straight);
            Double durationMin = route.duration() != null ? route.duration() / 60.0 : null;
            return new RoutedPath(true, distanceKm, durationMin, geometry);
        } catch (Exception e) {
            log.warn("Enrutado por OSRM falló, se usa línea recta: {}", e.getMessage());
            return fallback(straight);
        }
    }

    private RoutedPath fallback(double[][] straight) {
        List<double[]> geometry = new ArrayList<>(straight.length);
        for (double[] p : straight) geometry.add(p);
        return new RoutedPath(false, GeoUtils.routeLength(straight), null, geometry);
    }

    /** Muestreo uniforme preservando primer y último punto; misma técnica para waypoints de entrada y geometría de salida. */
    private <T> List<T> downsample(List<T> points, int max) {
        if (points.size() <= max) return points;
        List<T> result = new ArrayList<>(max);
        double step = (double) (points.size() - 1) / (max - 1);
        for (int i = 0; i < max; i++) {
            result.add(points.get((int) Math.round(i * step)));
        }
        return result;
    }
}
