package com.tecsharp.apis.pois.services.poi;

import com.tecsharp.apis.pois.dtos.NearbyPoiResponseDTO;
import com.tecsharp.apis.pois.dtos.PoiRequestDTO;
import com.tecsharp.apis.pois.dtos.PoiResponseDTO;
import com.tecsharp.apis.pois.dtos.RoutePoiRequestDTO;
import com.tecsharp.apis.pois.dtos.RoutePoiResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PoiService {

    PoiResponseDTO createPoi(PoiRequestDTO request);

    PoiResponseDTO getPOIById(UUID uuid);

    RoutePoiResponseDTO findPoisAlongRoute(RoutePoiRequestDTO request);

    NearbyPoiResponseDTO findNearby(double lat, double lng, double radiusKm, List<String> types, int limit);

    void deletePoi(UUID uuid);

    List<PoiResponseDTO> latest();
}
