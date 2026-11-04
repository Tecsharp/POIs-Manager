package com.tecsharp.apis.pois.services.auth;

import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.AuthResponse;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.LoginRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.RegisterRequest;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.UserDTO;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.exceptions.ConflictException;
import com.tecsharp.apis.pois.exceptions.UnauthorizedException;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.repositories.UserRepository;
import com.tecsharp.apis.pois.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = normalizeEmail(request.email());
        String password = request.password() == null ? "" : request.password();
        if (name.isEmpty() || name.length() > 80) {
            throw new BadRequestException("El nombre debe tener entre 1 y 80 caracteres");
        }
        if (email.length() > 160 || !EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Correo electrónico inválido");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new BadRequestException("La contraseña debe tener entre 8 y 72 caracteres");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }
        User user = userRepository.save(User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build());
        log.info("Usuario registrado: {}", email);
        return new AuthResponse(jwtService.generate(user), UserDTO.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .filter(u -> request.password() != null && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UnauthorizedException("Tu cuenta está deshabilitada");
        }
        return new AuthResponse(jwtService.generate(user), UserDTO.from(user));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
