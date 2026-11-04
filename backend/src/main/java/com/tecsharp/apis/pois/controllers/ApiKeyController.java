package com.tecsharp.apis.pois.controllers;

import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.ApiKeyDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.CreateKeyRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.CreatedKeyDTO;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.services.apikey.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public List<ApiKeyDTO> list(@AuthenticationPrincipal User user) {
        return apiKeyService.listForUser(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedKeyDTO create(@AuthenticationPrincipal User user, @RequestBody CreateKeyRequest request) {
        return apiKeyService.create(user, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal User user, @PathVariable("id") UUID id) {
        apiKeyService.revoke(user, id);
    }
}
