package com.tecsharp.apis.pois.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record PoiVoteSummaryDTO(@JsonProperty("poi_id") UUID poiId,
                                long upvotes,
                                long downvotes,
                                @JsonProperty("my_vote") Integer myVote) {
}
