package com.tecsharp.apis.pois.services.routing;

import com.tecsharp.apis.pois.dtos.WaypointDTO;
import com.tecsharp.apis.pois.services.routing.impl.RoutingServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingServiceImplTest {

    @Test
    void fallsBackToStraightLineWhenRoutingDisabled() {
        RoutingService service = new RoutingServiceImpl("https://router.project-osrm.org", false, 1000);
        List<WaypointDTO> waypoints = List.of(
                WaypointDTO.builder().lat(19.4326).lng(-99.1332).build(),
                WaypointDTO.builder().lat(19.1903).lng(-96.1533).build());

        RoutedPath path = service.route(waypoints);

        assertFalse(path.routed());
        assertEquals(2, path.geometry().size());
        assertArrayEquals(new double[]{19.4326, -99.1332}, path.geometry().get(0));
        assertTrue(path.distanceKm() > 200 && path.distanceKm() < 400, "distancia línea recta esperada ~290km");
    }

    @Test
    void fallsBackWhenFewerThanTwoWaypoints() {
        RoutingService service = new RoutingServiceImpl("https://router.project-osrm.org", true, 1000);
        RoutedPath path = service.route(List.of(WaypointDTO.builder().lat(19.0).lng(-99.0).build()));

        assertFalse(path.routed());
        assertEquals(1, path.geometry().size());
        assertEquals(0.0, path.distanceKm());
    }
}
