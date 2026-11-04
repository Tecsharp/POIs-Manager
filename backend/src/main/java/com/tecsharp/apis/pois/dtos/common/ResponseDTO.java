package com.tecsharp.apis.pois.dtos.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import static com.tecsharp.apis.pois.utils.Cons.CODE_CREATED;
import static com.tecsharp.apis.pois.utils.Cons.CODE_OK;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDTO<T extends Serializable> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private ErrorDTO errorDTO;
    private DataDTO<T> data;

    public static <T extends Serializable> ResponseDTO<T> ok(T object, String message) {
        return ResponseDTO.<T>builder()
                .data(DataDTO.<T>builder()
                        .code(CODE_OK)
                        .message(message)
                        .object(object)
                        .build())
                .build();
    }

    public static <T extends Serializable> ResponseDTO<T> created(T object, String message) {
        return ResponseDTO.<T>builder()
                .data(DataDTO.<T>builder()
                        .code(CODE_CREATED)
                        .message(message)
                        .object(object)
                        .build())
                .build();
    }

    public static <T extends Serializable> ResponseDTO<T> error(String errorCode, String errorMessage) {
        return ResponseDTO.<T>builder()
                .errorDTO(ErrorDTO.builder()
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .build())
                .build();
    }

    public static <T extends Serializable> ResponseDTO<T> error(String errorCode, String errorMessage, Map<String, String> details) {
        return ResponseDTO.<T>builder()
                .errorDTO(ErrorDTO.builder()
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .details(details)
                        .build())
                .build();
    }
}

