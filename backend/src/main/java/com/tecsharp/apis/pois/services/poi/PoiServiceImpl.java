package com.tecsharp.apis.pois.services.poi;

import com.tecsharp.apis.pois.dtos.*;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.exceptions.PoiNotFoundException;
import com.tecsharp.apis.pois.models.Poi;
import com.tecsharp.apis.pois.models.enums.PoiType;
import com.tecsharp.apis.pois.repositories.PoiRepository;
import com.tecsharp.apis.pois.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiServiceImpl implements PoiService {

    private final PoiRepository poiRepository;

    @Override
    public PoiResponseDTO createPoi(PoiRequestDTO request) {
        log.info("Creando POI: {}", request.getName());
        validatePoiRequest(request);

        Poi poi = Poi.builder()
                .name(request.getName())
                .type(request.getType())
                .lat(request.getLat())
                .lng(request.getLng())
                .address(request.getAddress())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .hours(request.getHours())
                .specialties(request.getSpecialties())
                .description(request.getDescription())
                .acceptsCards(request.getAcceptsCards())
                .rating(request.getRating())
                .build();

        Poi saved = poiRepository.save(poi);
        log.info("POI creado con ID: {}", saved.getId());
        return PoiResponseDTO.fromEntity(saved);
    }

    @Override
    public PoiResponseDTO getPOIById(UUID uuid) {
        log.info("Buscando POI con UUID: {}", uuid);
        Poi poi = poiRepository.findById(uuid).orElseThrow(() -> new PoiNotFoundException("POI no encontrado"));
        return PoiResponseDTO.fromEntity(poi);
    }

    @Override
    public NearbyPoiResponseDTO findNearby(double lat, double lng, double radiusKm, List<String> types, int limit) {
        validateCoordinates(lat, lng);
        if (radiusKm < 0.5 || radiusKm > 100) {
            throw new BadRequestException("radius_km debe estar entre 0.5 y 100");
        }
        validateMaxResults(limit);
        double latDeg = radiusKm / 111.0;
        double lngDeg = radiusKm / (111.0 * Math.max(0.01, Math.cos(Math.toRadians(Math.min(89, Math.abs(lat) + latDeg)))));
        Set<PoiType> requestedTypes = parseRequestedTypes(types);
        double[][] point = {{lat, lng}, {lat, lng}};

        List<RoutePoiItemDTO> items = poiRepository.findWithinBoundingBox(lat - latDeg, lat + latDeg, lng - lngDeg, lng + lngDeg)
                .stream()
                .filter(poi -> matchesFilters(poi, false, requestedTypes, null, false))
                .map(poi -> toItemWithDistance(poi, point))
                .filter(item -> item.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(RoutePoiItemDTO::getDistanceKm))
                .limit(limit)
                .toList();

        Map<String, Long> byType = items.stream()
                .collect(Collectors.groupingBy(RoutePoiItemDTO::getType, Collectors.counting()));
        return new NearbyPoiResponseDTO(items.size(), lat, lng, radiusKm, items, byType);
    }

    @Override
    public void deletePoi(UUID uuid) {
        Poi poi = poiRepository.findById(uuid).orElseThrow(() -> new PoiNotFoundException("POI no encontrado"));
        poiRepository.delete(poi);
        log.info("POI eliminado: {}", uuid);
    }

    @Override
    public List<PoiResponseDTO> latest() {
        return poiRepository.findTop100ByOrderByCreatedAtDesc().stream().map(PoiResponseDTO::fromEntity).toList();
    }

    @Override
    public RoutePoiResponseDTO findPoisAlongRoute(RoutePoiRequestDTO request) {
        validateRouteRequest(request);

        List<WaypointDTO> waypoints = request.getWaypoints();
        double bufferKm = request.getBufferKm() != null ? request.getBufferKm() : 8.0;
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 50;
        boolean includeClosed = request.getIncludeClosed() != null && request.getIncludeClosed();
        Double minRating = request.getMinRating();
        boolean verifiedOnly = request.getVerifiedOnly() != null && request.getVerifiedOnly();

        double[][] waypointsArray = waypoints.stream()
                .map(w -> new double[]{w.getLat(), w.getLng()})
                .toArray(double[][]::new);

        double routeLengthKm = Math.round(GeoUtils.routeLength(waypointsArray) * 10.0) / 10.0;

        double[] bbox = calculateBoundingBox(waypoints, bufferKm);

        List<Poi> candidates = poiRepository.findWithinBoundingBox(bbox[0], bbox[1], bbox[2], bbox[3]);
        log.info("Candidatos encontrados en bounding box: {}", candidates.size());
        Set<PoiType> requestedTypes = parseRequestedTypes(request.getTypes());

        List<RoutePoiItemDTO> poiItems = candidates.stream()
                .filter(poi -> matchesFilters(poi, includeClosed, requestedTypes, minRating, verifiedOnly))
                .map(poi -> toItemWithDistance(poi, waypointsArray))
                .filter(item -> item.getDistanceKm() <= bufferKm)
                .sorted(Comparator.comparingDouble(RoutePoiItemDTO::getDistanceKm))
                .limit(maxResults)
                .toList();

        Map<String, Long> byType = poiItems.stream()
                .collect(Collectors.groupingBy(RoutePoiItemDTO::getType, Collectors.counting()));

        return RoutePoiResponseDTO.builder()
                .total(poiItems.size())
                .routeLengthKm(routeLengthKm)
                .bufferKm(bufferKm)
                .pois(poiItems)
                .byType(byType)
                .build();
    }

    private double[] calculateBoundingBox(List<WaypointDTO> waypoints, double bufferKm) {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;
        for (WaypointDTO wp : waypoints) {
            minLat = Math.min(minLat, wp.getLat());
            maxLat = Math.max(maxLat, wp.getLat());
            minLng = Math.min(minLng, wp.getLng());
            maxLng = Math.max(maxLng, wp.getLng());
        }
        double bufferDeg = bufferKm / 111.0;
        // Un grado de longitud mide menos km lejos del ecuador: el margen en lng debe crecer con la latitud.
        double maxAbsLat = Math.min(89, Math.max(Math.abs(minLat), Math.abs(maxLat)) + bufferDeg);
        double lngBufferDeg = bufferKm / (111.0 * Math.max(0.01, Math.cos(Math.toRadians(maxAbsLat))));
        return new double[]{minLat - bufferDeg, maxLat + bufferDeg, minLng - lngBufferDeg, maxLng + lngBufferDeg};
    }

    private Set<PoiType> parseRequestedTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptySet();
        }
        return types.stream()
                .map(t -> {
                    try {
                        return PoiType.valueOf(t.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Tipo de POI desconocido: {}", t);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean matchesFilters(Poi poi, boolean includeClosed, Set<PoiType> requestedTypes,
                                   Double minRating, boolean verifiedOnly) {
        if (!includeClosed && Boolean.FALSE.equals(poi.getActive())) return false;
        if (!requestedTypes.isEmpty() && !requestedTypes.contains(poi.getType())) return false;
        if (minRating != null && poi.getRating() != null && poi.getRating() < minRating) return false;
        return !verifiedOnly || Boolean.TRUE.equals(poi.getVerified());
    }

    private RoutePoiItemDTO toItemWithDistance(Poi poi, double[][] waypointsArray) {
        double dist = Math.round(GeoUtils.distanceToPolyline(poi.getLat(), poi.getLng(), waypointsArray) * 10.0) / 10.0;
        return RoutePoiItemDTO.builder()
                .id(poi.getId())
                .name(poi.getName())
                .type(poi.getType().name().toLowerCase())
                .lat(poi.getLat())
                .lng(poi.getLng())
                .address(poi.getAddress())
                .rating(poi.getRating())
                .verified(poi.getVerified())
                .distanceKm(dist)
                .build();
    }

    private void validateRouteRequest(RoutePoiRequestDTO request) {
        validateWaypoints(request.getWaypoints());
        validateBufferKm(request.getBufferKm());
        validateMaxResults(request.getMaxResults());
        validateMinRating(request.getMinRating());
    }

    private void validateWaypoints(List<WaypointDTO> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new BadRequestException("Se requieren al menos 2 waypoints");
        }
        if (waypoints.size() > 500) {
            throw new BadRequestException("Se permiten máximo 500 waypoints");
        }
        for (WaypointDTO wp : waypoints) {
            if (wp == null || wp.getLat() == null || wp.getLng() == null) {
                throw new BadRequestException("Cada waypoint debe tener lat y lng");
            }
            validateCoordinates(wp.getLat(), wp.getLng());
        }
    }

    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BadRequestException("Coordenadas fuera de rango");
        }
    }

    private void validatePoiRequest(PoiRequestDTO request) {
        if (request.getName() == null || request.getName().isBlank() || request.getName().length() > 200) {
            throw new BadRequestException("name es requerido (máx. 200 caracteres)");
        }
        if (request.getType() == null) {
            throw new BadRequestException("type es requerido");
        }
        if (request.getLat() == null || request.getLng() == null) {
            throw new BadRequestException("lat y lng son requeridos");
        }
        validateCoordinates(request.getLat(), request.getLng());
        if (request.getRating() != null) {
            validateMinRating(request.getRating());
        }
    }

    private void validateBufferKm(Double bufferKm) {
        if (bufferKm != null && (bufferKm < 0.5 || bufferKm > 50)) {
            throw new BadRequestException("buffer_km debe estar entre 0.5 y 50");
        }
    }

    private void validateMaxResults(Integer maxResults) {
        if (maxResults != null && (maxResults < 1 || maxResults > 200)) {
            throw new BadRequestException("max_results debe estar entre 1 y 200");
        }
    }

    private void validateMinRating(Double minRating) {
        if (minRating != null && (minRating < 1 || minRating > 5)) {
            throw new BadRequestException("min_rating debe estar entre 1 y 5");
        }
    }
}
