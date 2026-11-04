package com.tecsharp.apis.pois.dtos.common;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataDTO<T extends Serializable> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private T object;
}

