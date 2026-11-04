package com.tecsharp.apis.pois.utils;

/**
 * Utilidades geográficas para cálculos de distancia usando la fórmula de Haversine
 * y distancia punto-a-segmento sobre la superficie terrestre.
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
        /* Utility class */
    }

    /**
     * Calcula la distancia en km entre dos puntos usando la fórmula de Haversine.
     */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calcula la distancia mínima en km de un punto a un segmento de línea definido por dos puntos.
     * Proyecta el punto sobre el segmento y calcula la distancia al punto más cercano del segmento.
     */
    public static double distanceToSegment(double pLat, double pLng,
                                           double aLat, double aLng,
                                           double bLat, double bLng) {
        // Convertir a radianes para la proyección
        double dx = bLng - aLng;
        double dy = bLat - aLat;

        if (dx == 0 && dy == 0) {
            // El segmento es un punto
            return haversine(pLat, pLng, aLat, aLng);
        }

        // Parámetro t de la proyección del punto sobre la línea [0, 1]
        double t = ((pLng - aLng) * dx + (pLat - aLat) * dy) / (dx * dx + dy * dy);
        t = Math.clamp(t, 0, 1);

        // Punto más cercano en el segmento
        double closestLat = aLat + t * dy;
        double closestLng = aLng + t * dx;

        return haversine(pLat, pLng, closestLat, closestLng);
    }

    /**
     * Calcula la distancia mínima de un punto a una polilínea (conjunto de segmentos).
     */
    public static double distanceToPolyline(double pLat, double pLng,
                                            double[][] waypoints) {
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < waypoints.length - 1; i++) {
            double dist = distanceToSegment(pLat, pLng,
                    waypoints[i][0], waypoints[i][1],
                    waypoints[i + 1][0], waypoints[i + 1][1]);
            if (dist < minDistance) {
                minDistance = dist;
            }
        }

        return minDistance;
    }

    /**
     * Calcula la longitud total de una ruta definida por waypoints en km.
     */
    public static double routeLength(double[][] waypoints) {
        double total = 0;
        for (int i = 0; i < waypoints.length - 1; i++) {
            total += haversine(waypoints[i][0], waypoints[i][1],
                    waypoints[i + 1][0], waypoints[i + 1][1]);
        }
        return total;
    }
}

