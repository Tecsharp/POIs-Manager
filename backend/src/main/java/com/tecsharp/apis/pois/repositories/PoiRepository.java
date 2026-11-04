package com.tecsharp.apis.pois.repositories;

import com.tecsharp.apis.pois.models.Poi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PoiRepository extends JpaRepository<Poi, UUID> {

    /**
     * Busca POIs dentro de un bounding box geográfico.
     * El filtrado fino por distancia al corredor se hace en el servicio.
     */
    @Query("SELECT p FROM Poi p WHERE p.lat BETWEEN :minLat AND :maxLat AND p.lng BETWEEN :minLng AND :maxLng")
    List<Poi> findWithinBoundingBox(@Param("minLat") double minLat,
                                    @Param("maxLat") double maxLat,
                                    @Param("minLng") double minLng,
                                    @Param("maxLng") double maxLng);

    List<Poi> findTop100ByOrderByCreatedAtDesc();
}
