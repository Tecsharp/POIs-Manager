package com.tecsharp.apis.pois.repositories;

import com.tecsharp.apis.pois.models.ApiKeyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyUsageRepository extends JpaRepository<ApiKeyUsage, Long> {

    /** Incremento atómico (upsert) del contador diario. Devuelve el total del día tras sumar. */
    @Query(value = """
            INSERT INTO api_key_usage (api_key_id, usage_date, requests) VALUES (:keyId, :day, 1)
            ON CONFLICT (api_key_id, usage_date) DO UPDATE SET requests = api_key_usage.requests + 1
            RETURNING requests
            """, nativeQuery = true)
    int increment(@Param("keyId") UUID keyId, @Param("day") LocalDate day);

    List<ApiKeyUsage> findByApiKeyIdInAndUsageDateGreaterThanEqual(Collection<UUID> keyIds, LocalDate from);

    @Query("SELECT COALESCE(SUM(u.requests), 0) FROM ApiKeyUsage u WHERE u.usageDate = :day")
    long sumByDay(@Param("day") LocalDate day);

    void deleteByApiKeyIdIn(Collection<UUID> keyIds);
}
