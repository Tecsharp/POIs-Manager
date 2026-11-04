package com.tecsharp.apis.pois.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaypointDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Double lat;
    private Double lng;
}

