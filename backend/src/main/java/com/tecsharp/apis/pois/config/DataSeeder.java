package com.tecsharp.apis.pois.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsharp.apis.pois.dtos.PoiRequestDTO;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.models.enums.Role;
import com.tecsharp.apis.pois.repositories.PoiRepository;
import com.tecsharp.apis.pois.repositories.UserRepository;
import com.tecsharp.apis.pois.services.poi.PoiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Arranque: sincroniza el administrador con ADMIN_EMAIL/ADMIN_PASSWORD (generados por setup.sh)
 * y carga POIs de ejemplo si la tabla está vacía.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PoiRepository poiRepository;
    private final PoiService poiService;
    private final PasswordEncoder passwordEncoder;

    @Value("${pois.admin.email:}")
    private String adminEmail;

    @Value("${pois.admin.password:}")
    private String adminPassword;

    @Value("${pois.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedAdmin();
        if (seedEnabled && poiRepository.count() == 0) {
            seedPois();
        }
    }

    private void seedAdmin() {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL/ADMIN_PASSWORD no definidos: no se crea administrador");
            return;
        }
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        User admin = userRepository.findByEmail(email).orElseGet(() -> User.builder().email(email).name("Administrador").build());
        // La contraseña del .env es la fuente de verdad: así lo que imprime setup.sh siempre funciona.
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("Administrador listo: {}", email);
    }

    private void seedPois() throws Exception {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource("seed/pois.json").getInputStream()) {
            List<PoiRequestDTO> pois = mapper.readValue(in, new TypeReference<>() {});
            pois.forEach(poiService::createPoi);
            log.info("{} POIs de ejemplo cargados", pois.size());
        }
    }
}
