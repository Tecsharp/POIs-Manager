package com.tecsharp.apis.pois.controllers;

import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.AuthResponse;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.LoginRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.RegisterRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.UserDTO;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.services.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDTO me(@AuthenticationPrincipal User user) {
        return UserDTO.from(user);
    }
}
