package com.tecsharp.apis.pois.services.poi;

import com.tecsharp.apis.pois.dtos.*;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.exceptions.PoiNotFoundException;
import com.tecsharp.apis.pois.models.Poi;
import com.tecsharp.apis.pois.models.PoiVote;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.models.enums.PoiType;
import com.tecsharp.apis.pois.models.enums.Role;
import com.tecsharp.apis.pois.repositories.PoiRepository;
import com.tecsharp.apis.pois.repositories.PoiVoteRepository;
import com.tecsharp.apis.pois.services.routing.RoutedPath;
import com.tecsharp.apis.pois.services.routing.RoutingService;
import com.tecsharp.apis.pois.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    private final PoiVoteRepository poiVoteRepository;
    private final RoutingService routingService;

    @Override
    public PoiResponseDTO createPoi(PoiRequestDTO request, User creator) {
        log.info("Creando POI: {} (autor: {})", request.getName(), creator != null ? creator.getEmail() : "sistema");
        validatePoiRequest(request);

        // Los lugares del seed (creator=null) y los que da de alta un admin se muestran como
        // "curados por Rutea" (sin nombre de autor) y verificados; el resto son aportes de la comunidad.
        boolean official = creator == null || creator.getRole() == Role.ADMIN;

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
                .verified(official)
                .createdByUserId(creator != null ? creator.getId() : null)
                .createdByName(official ? null : creator.getName())
                .build();

        Poi saved = poiRepository.save(poi);
        log.info("POI creado con ID: {}", saved.getId());
        return PoiResponseDTO.fromEntity(saved);
    }

    @Override
    public PoiResponseDTO getPOIById(UUID uuid, UUID currentUserId) {
        log.info("Buscando POI con UUID: {}", uuid);
        Poi poi = poiRepository.findById(uuid).orElseThrow(() -> new PoiNotFoundException("POI no encontrado"));
        PoiVoteSummaryDTO votes = voteSummary(uuid, currentUserId);
        return PoiResponseDTO.fromEntity(poi).toBuilder()
                .upvotes(votes.upvotes())
                .downvotes(votes.downvotes())
                .myVote(votes.myVote())
                .build();
    }

    @Override
    public NearbyPoiResponseDTO findNearby(double lat, double lng, double radiusKm, List<String> types, int limit, UUID currentUserId) {
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
        attachVotes(items, currentUserId);

        Map<String, Long> byType = items.stream()
                .collect(Collectors.groupingBy(RoutePoiItemDTO::getType, Collectors.counting()));
        return new NearbyPoiResponseDTO(items.size(), lat, lng, radiusKm, items, byType);
    }

    @Override
    public DirectionsDTO directions(double fromLat, double fromLng, double toLat, double toLng) {
        validateCoordinates(fromLat, fromLng);
        validateCoordinates(toLat, toLng);
        List<WaypointDTO> waypoints = List.of(
                WaypointDTO.builder().lat(fromLat).lng(fromLng).build(),
                WaypointDTO.builder().lat(toLat).lng(toLng).build());
        RoutedPath path = routingService.route(waypoints);
        List<WaypointDTO> geometry = path.geometry().stream()
                .map(p -> WaypointDTO.builder().lat(p[0]).lng(p[1]).build())
                .toList();
        Double durationMin = path.durationMin() != null ? round1(path.durationMin()) : null;
        return new DirectionsDTO(path.routed(), round1(path.distanceKm()), durationMin, geometry);
    }

    @Override
    @Transactional
    public PoiVoteSummaryDTO vote(User user, UUID poiId, int value) {
        if (value != 1 && value != -1) {
            throw new BadRequestException("value debe ser 1 o -1");
        }
        if (!poiRepository.existsById(poiId)) {
            throw new PoiNotFoundException("POI no encontrado");
        }
        PoiVote existing = poiVoteRepository.findByPoiIdAndUserId(poiId, user.getId()).orElse(null);
        if (existing != null) {
            existing.setValue(value);
            poiVoteRepository.save(existing);
        } else {
            poiVoteRepository.save(PoiVote.builder().poiId(poiId).userId(user.getId()).value(value).build());
        }
        return voteSummary(poiId, user.getId());
    }

    @Override
    @Transactional
    public PoiVoteSummaryDTO removeVote(User user, UUID poiId) {
        if (!poiRepository.existsById(poiId)) {
            throw new PoiNotFoundException("POI no encontrado");
        }
        poiVoteRepository.deleteByPoiIdAndUserId(poiId, user.getId());
        return voteSummary(poiId, user.getId());
    }

    @Override
    public void deletePoi(UUID uuid) {
        Poi poi = poiRepository.findById(uuid).orElseThrow(() -> new PoiNotFoundException("POI no encontrado"));
        poiVoteRepository.deleteByPoiId(uuid);
        poiRepository.delete(poi);
        log.info("POI eliminado: {}", uuid);
    }

    @Override
    public List<PoiResponseDTO> latest() {
        return poiRepository.findTop100ByOrderByCreatedAtDesc().stream().map(PoiResponseDTO::fromEntity).toList();
    }

    @Override
    public RoutePoiResponseDTO findPoisAlongRoute(RoutePoiRequestDTO request, UUID currentUserId) {
        validateRouteRequest(request);

        double bufferKm = request.getBufferKm() != null ? request.getBufferKm() : 8.0;
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 50;
        boolean includeClosed = request.getIncludeClosed() != null && request.getIncludeClosed();
        Double minRating = request.getMinRating();
        boolean verifiedOnly = request.getVerifiedOnly() != null && request.getVerifiedOnly();

        RoutedPath routedPath = routingService.route(request.getWaypoints());
        double[][] routeGeometry = routedPath.geometry().toArray(double[][]::new);
        double routeLengthKm = round1(routedPath.distanceKm());

        double[] bbox = calculateBoundingBox(routeGeometry, bufferKm);

        List<Poi> candidates = poiRepository.findWithinBoundingBox(bbox[0], bbox[1], bbox[2], bbox[3]);
        log.info("Candidatos encontrados en bounding box: {}", candidates.size());
        Set<PoiType> requestedTypes = parseRequestedTypes(request.getTypes());

        List<RoutePoiItemDTO> poiItems = candidates.stream()
                .filter(poi -> matchesFilters(poi, includeClosed, requestedTypes, minRating, verifiedOnly))
                .map(poi -> toItemWithDistance(poi, routeGeometry))
                .filter(item -> item.getDistanceKm() <= bufferKm)
                .sorted(Comparator.comparingDouble(RoutePoiItemDTO::getDistanceKm))
                .limit(maxResults)
                .toList();
        attachVotes(poiItems, currentUserId);

        Map<String, Long> byType = poiItems.stream()
                .collect(Collectors.groupingBy(RoutePoiItemDTO::getType, Collectors.counting()));

        List<WaypointDTO> geometryDto = routedPath.geometry().stream()
                .map(p -> WaypointDTO.builder().lat(p[0]).lng(p[1]).build())
                .toList();

        return RoutePoiResponseDTO.builder()
                .total(poiItems.size())
                .routeLengthKm(routeLengthKm)
                .bufferKm(bufferKm)
                .routed(routedPath.routed())
                .routeGeometry(geometryDto)
                .pois(poiItems)
                .byType(byType)
                .build();
    }

    private PoiVoteSummaryDTO voteSummary(UUID poiId, UUID currentUserId) {
        long up = 0, down = 0;
        var row = poiVoteRepository.countByPoiIds(List.of(poiId)).stream().findFirst();
        if (row.isPresent()) {
            up = row.get().getUpvotes();
            down = row.get().getDownvotes();
        }
        Integer myVote = currentUserId == null ? null
                : poiVoteRepository.findByPoiIdAndUserId(poiId, currentUserId).map(PoiVote::getValue).orElse(null);
        return new PoiVoteSummaryDTO(poiId, up, down, myVote);
    }

    /** Completa upvotes/downvotes/my_vote en lote (dos consultas) en vez de una por POI listado. */
    private void attachVotes(List<RoutePoiItemDTO> items, UUID currentUserId) {
        if (items.isEmpty()) return;
        List<UUID> ids = items.stream().map(RoutePoiItemDTO::getId).toList();
        Map<UUID, PoiVoteRepository.VoteCounts> counts = poiVoteRepository.countByPoiIds(ids).stream()
                .collect(Collectors.toMap(PoiVoteRepository.VoteCounts::getPoiId, c -> c));
        Map<UUID, Integer> myVotes = currentUserId == null ? Map.of()
                : poiVoteRepository.findByUserIdAndPoiIdIn(currentUserId, ids).stream()
                        .collect(Collectors.toMap(PoiVote::getPoiId, PoiVote::getValue));
        for (RoutePoiItemDTO item : items) {
            PoiVoteRepository.VoteCounts c = counts.get(item.getId());
            item.setUpvotes(c != null ? c.getUpvotes() : 0L);
            item.setDownvotes(c != null ? c.getDownvotes() : 0L);
            item.setMyVote(myVotes.get(item.getId()));
        }
    }

    private double[] calculateBoundingBox(double[][] points, double bufferKm) {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;
        for (double[] p : points) {
            minLat = Math.min(minLat, p[0]);
            maxLat = Math.max(maxLat, p[0]);
            minLng = Math.min(minLng, p[1]);
            maxLng = Math.max(maxLng, p[1]);
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

    private RoutePoiItemDTO toItemWithDistance(Poi poi, double[][] routeGeometry) {
        double dist = round1(GeoUtils.distanceToPolyline(poi.getLat(), poi.getLng(), routeGeometry));
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
                .createdBy(poi.getCreatedByName())
                .build();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
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
