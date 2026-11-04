package com.tecsharp.apis.pois.repositories;

import com.tecsharp.apis.pois.models.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    @Query("SELECT k FROM ApiKey k JOIN FETCH k.user WHERE k.keyHash = :hash")
    Optional<ApiKey> findByKeyHashWithUser(@Param("hash") String hash);

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndActiveTrue(UUID userId);

    long countByActiveTrue();

    @Query("SELECT k FROM ApiKey k JOIN FETCH k.user ORDER BY k.createdAt DESC")
    List<ApiKey> findAllWithUser();

    @Modifying
    @Query("UPDATE ApiKey k SET k.lastUsedAt = :now WHERE k.id = :id")
    void touch(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
