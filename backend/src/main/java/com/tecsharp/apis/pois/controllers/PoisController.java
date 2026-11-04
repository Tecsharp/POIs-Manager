package com.tecsharp.apis.pois.controllers;

import com.tecsharp.apis.pois.dtos.DirectionsDTO;
import com.tecsharp.apis.pois.dtos.NearbyPoiResponseDTO;
import com.tecsharp.apis.pois.dtos.PoiRequestDTO;
import com.tecsharp.apis.pois.dtos.PoiResponseDTO;
import com.tecsharp.apis.pois.dtos.PoiVoteSummaryDTO;
import com.tecsharp.apis.pois.dtos.RoutePoiRequestDTO;
import com.tecsharp.apis.pois.dtos.RoutePoiResponseDTO;
import com.tecsharp.apis.pois.dtos.VoteRequest;
import com.tecsharp.apis.pois.dtos.common.ResponseDTO;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.models.enums.PoiType;
import com.tecsharp.apis.pois.services.poi.PoiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@RequestMapping("/pois")
@RequiredArgsConstructor
public class PoisController {
    // Inyección de services
    private final PoiService poiService;

    @GetMapping("/status")
    public ResponseEntity<Void> getStatus() {
        log.info("API Saludable - El servicio está funcionando correctamente");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/types")
    public List<String> getTypes() {
        return Arrays.stream(PoiType.values()).map(t -> t.name().toLowerCase()).toList();
    }

    @GetMapping("/nearby")
    public NearbyPoiResponseDTO findNearby(@RequestParam("lat") double lat,
                                           @RequestParam("lng") double lng,
                                           @RequestParam(value = "radius_km", defaultValue = "10") double radiusKm,
                                           @RequestParam(value = "type", required = false) List<String> types,
                                           @RequestParam(value = "limit", defaultValue = "50") int limit,
                                           @AuthenticationPrincipal User user) {
        return poiService.findNearby(lat, lng, radiusKm, types, limit, userId(user));
    }

    /** Ruta real por carretera (OSRM) entre dos puntos, p. ej. desde la ubicación del usuario hasta un POI. */
    @GetMapping("/directions")
    public DirectionsDTO directions(@RequestParam("from_lat") double fromLat,
                                    @RequestParam("from_lng") double fromLng,
                                    @RequestParam("to_lat") double toLat,
                                    @RequestParam("to_lng") double toLng) {
        return poiService.directions(fromLat, fromLng, toLat, toLng);
    }

    /** Crea un POI. Requiere cuenta (cualquier usuario autenticado); queda marcado como aportación de la comunidad. */
    @PostMapping
    public ResponseEntity<ResponseDTO<PoiResponseDTO>> createPoi(@RequestBody PoiRequestDTO request,
                                                                  @AuthenticationPrincipal User user) {
        log.info("Usuario {} creando POI: {}", user.getEmail(), request.getName());
        PoiResponseDTO poi = poiService.createPoi(request, user);
        var response = ResponseDTO.created(poi, "POI creado exitosamente");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseDTO<PoiResponseDTO>> getPOIById(@PathVariable("uuid") UUID uuid,
                                                                   @AuthenticationPrincipal User user) {
        PoiResponseDTO poi = poiService.getPOIById(uuid, userId(user));
        var response = ResponseDTO.ok( poi, "POI encontrado");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deletePoi(@PathVariable("uuid") UUID uuid) {
        poiService.deletePoi(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uuid}/vote")
    public PoiVoteSummaryDTO vote(@PathVariable("uuid") UUID uuid, @RequestBody VoteRequest request,
                                  @AuthenticationPrincipal User user) {
        if (request.value() == null) {
            throw new BadRequestException("value es requerido (1 o -1)");
        }
        return poiService.vote(user, uuid, request.value());
    }

    @DeleteMapping("/{uuid}/vote")
    public PoiVoteSummaryDTO removeVote(@PathVariable("uuid") UUID uuid, @AuthenticationPrincipal User user) {
        return poiService.removeVote(user, uuid);
    }

    @PostMapping("/route")
    public ResponseEntity<RoutePoiResponseDTO> findPoisAlongRoute(@RequestBody RoutePoiRequestDTO request,
                                                                   @AuthenticationPrincipal User user) {
        log.info("Buscando POIs a lo largo de una ruta con {} waypoints", request.getWaypoints() != null ? request.getWaypoints().size() : 0);
        RoutePoiResponseDTO response = poiService.findPoisAlongRoute(request, userId(user));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private UUID userId(User user) {
        return user != null ? user.getId() : null;
    }
}
