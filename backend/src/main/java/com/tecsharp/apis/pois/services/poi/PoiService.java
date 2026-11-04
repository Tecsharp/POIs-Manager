package com.tecsharp.apis.pois.services.poi;

import com.tecsharp.apis.pois.dtos.DirectionsDTO;
import com.tecsharp.apis.pois.dtos.NearbyPoiResponseDTO;
import com.tecsharp.apis.pois.dtos.PoiRequestDTO;
import com.tecsharp.apis.pois.dtos.PoiResponseDTO;
import com.tecsharp.apis.pois.dtos.PoiVoteSummaryDTO;
import com.tecsharp.apis.pois.dtos.RoutePoiRequestDTO;
import com.tecsharp.apis.pois.dtos.RoutePoiResponseDTO;
import com.tecsharp.apis.pois.models.User;

import java.util.List;
import java.util.UUID;

public interface PoiService {

    PoiResponseDTO createPoi(PoiRequestDTO request, User creator);

    PoiResponseDTO getPOIById(UUID uuid, UUID currentUserId);

    RoutePoiResponseDTO findPoisAlongRoute(RoutePoiRequestDTO request, UUID currentUserId);

    NearbyPoiResponseDTO findNearby(double lat, double lng, double radiusKm, List<String> types, int limit, UUID currentUserId);

    DirectionsDTO directions(double fromLat, double fromLng, double toLat, double toLng);

    PoiVoteSummaryDTO vote(User user, UUID poiId, int value);

    PoiVoteSummaryDTO removeVote(User user, UUID poiId);

    void deletePoi(UUID uuid);

    List<PoiResponseDTO> latest();
}
