package com.tecsharp.apis.pois.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsharp.apis.pois.dtos.PoiRequestDTO;
import com.tecsharp.apis.pois.models.Poi;
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
import java.util.ArrayList;
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

    private static final int SEED_BATCH_SIZE = 1000;

    /**
     * El seed puede traer desde unas decenas (demo) hasta cientos de miles de lugares (importación
     * de OSM, ver tools/import_osm_pois.py). Insertarlos uno por uno vía el servicio público —como
     * hacía antes— tardaría demasiado y bloquearía el arranque; por eso aquí se insertan directo
     * por lote en un hilo aparte, para que la app quede lista (healthcheck) sin esperar a que
     * termine de sembrar.
     */
    private void seedPois() throws Exception {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<PoiRequestDTO> pois;
        try (InputStream in = new ClassPathResource("seed/pois.json").getInputStream()) {
            pois = mapper.readValue(in, new TypeReference<>() {});
        }
        log.info("Sembrando {} lugares en segundo plano…", pois.size());
        Thread seeder = new Thread(() -> seedPoisInBatches(pois), "poi-seeder");
        seeder.setDaemon(true);
        seeder.start();
    }

    private void seedPoisInBatches(List<PoiRequestDTO> pois) {
        long startMs = System.currentTimeMillis();
        int saved = 0;
        int skipped = 0;
        List<Poi> batch = new ArrayList<>(SEED_BATCH_SIZE);
        for (PoiRequestDTO p : pois) {
            if (p.getName() == null || p.getName().isBlank() || p.getType() == null
                    || p.getLat() == null || p.getLng() == null) {
                skipped++;
                continue;
            }
            // Lugares del seed = curados/oficiales (igual que un admin los diera de alta a mano).
            batch.add(Poi.builder()
                    .name(p.getName())
                    .type(p.getType())
                    .lat(p.getLat())
                    .lng(p.getLng())
                    .address(p.getAddress())
                    .phone(p.getPhone())
                    .website(p.getWebsite())
                    .hours(p.getHours())
                    .specialties(p.getSpecialties())
                    .description(p.getDescription())
                    .acceptsCards(p.getAcceptsCards())
                    .rating(p.getRating())
                    .verified(true)
                    .build());
            if (batch.size() >= SEED_BATCH_SIZE) {
                poiRepository.saveAll(batch);
                saved += batch.size();
                batch.clear();
                log.info("Sembrando lugares… {}/{}", saved, pois.size());
            }
        }
        if (!batch.isEmpty()) {
            poiRepository.saveAll(batch);
            saved += batch.size();
        }
        long seconds = (System.currentTimeMillis() - startMs) / 1000;
        log.info("Siembra terminada: {} lugares guardados, {} omitidos (datos incompletos), {} s", saved, skipped, seconds);
    }
}
