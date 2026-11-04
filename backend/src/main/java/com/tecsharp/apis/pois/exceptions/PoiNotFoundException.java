package com.tecsharp.apis.pois.exceptions;

public class PoiNotFoundException extends RuntimeException {
    public PoiNotFoundException(String message) {
        super(message);
    }
}
