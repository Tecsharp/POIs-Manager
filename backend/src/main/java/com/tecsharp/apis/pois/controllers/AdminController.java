package com.tecsharp.apis.pois.controllers;

import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.AdminUserDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.ApiKeyDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.StatsDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.UpdateKeyRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.UpdateUserRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.UserDTO;
import com.tecsharp.apis.pois.dtos.PoiResponseDTO;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.services.admin.AdminService;
import com.tecsharp.apis.pois.services.apikey.ApiKeyService;
import com.tecsharp.apis.pois.services.poi.PoiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ApiKeyService apiKeyService;
    private final PoiService poiService;

    @GetMapping("/stats")
    public StatsDTO stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    public List<AdminUserDTO> users() {
        return adminService.users();
    }

    @PatchMapping("/users/{id}")
    public UserDTO updateUser(@AuthenticationPrincipal User admin, @PathVariable("id") UUID id,
                              @RequestBody UpdateUserRequest request) {
        if (request.enabled() == null) {
            throw new BadRequestException("enabled es requerido");
        }
        return adminService.setEnabled(admin, id, request.enabled());
    }

    @GetMapping("/keys")
    public List<ApiKeyDTO> keys() {
        return apiKeyService.listAll();
    }

    @PatchMapping("/keys/{id}")
    public ApiKeyDTO updateKey(@PathVariable("id") UUID id, @RequestBody UpdateKeyRequest request) {
        return apiKeyService.update(id, request.active(), request.dailyLimit());
    }

    @GetMapping("/pois")
    public List<PoiResponseDTO> latestPois() {
        return poiService.latest();
    }
}
