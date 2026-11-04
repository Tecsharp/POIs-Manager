package com.tecsharp.apis.pois.repositories;

import com.tecsharp.apis.pois.models.PoiVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PoiVoteRepository extends JpaRepository<PoiVote, Long> {

    Optional<PoiVote> findByPoiIdAndUserId(UUID poiId, UUID userId);

    void deleteByPoiIdAndUserId(UUID poiId, UUID userId);

    void deleteByPoiId(UUID poiId);

    List<PoiVote> findByUserIdAndPoiIdIn(UUID userId, Collection<UUID> poiIds);

    @Query("SELECT v.poiId AS poiId, SUM(CASE WHEN v.value = 1 THEN 1 ELSE 0 END) AS upvotes, "
            + "SUM(CASE WHEN v.value = -1 THEN 1 ELSE 0 END) AS downvotes "
            + "FROM PoiVote v WHERE v.poiId IN :poiIds GROUP BY v.poiId")
    List<VoteCounts> countByPoiIds(@Param("poiIds") Collection<UUID> poiIds);

    interface VoteCounts {
        UUID getPoiId();
        Long getUpvotes();
        Long getDownvotes();
    }
}
