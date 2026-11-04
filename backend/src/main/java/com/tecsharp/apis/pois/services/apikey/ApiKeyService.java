package com.tecsharp.apis.pois.services.apikey;

import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.ApiKeyDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.CreatedKeyDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.DailyUsage;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.exceptions.PoiNotFoundException;
import com.tecsharp.apis.pois.models.ApiKey;
import com.tecsharp.apis.pois.models.ApiKeyUsage;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.repositories.ApiKeyRepository;
import com.tecsharp.apis.pois.repositories.ApiKeyUsageRepository;
import com.tecsharp.apis.pois.security.ApiKeys;
import com.tecsharp.apis.pois.security.FixedWindowRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiKeyService {

    public record Consumption(UUID keyId, FixedWindowRateLimiter.Result quota) {}

    private static final int HISTORY_DAYS = 7;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageRepository usageRepository;
    private final int defaultDailyLimit;
    private final int maxKeysPerUser;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ApiKeyUsageRepository usageRepository,
                         @Value("${pois.api-key.daily-limit:1000}") int defaultDailyLimit,
                         @Value("${pois.api-key.max-per-user:3}") int maxKeysPerUser) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageRepository = usageRepository;
        this.defaultDailyLimit = defaultDailyLimit;
        this.maxKeysPerUser = maxKeysPerUser;
    }

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    @Transactional
    public CreatedKeyDTO create(User user, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 60) {
            throw new BadRequestException("El nombre de la key debe tener entre 1 y 60 caracteres");
        }
        if (apiKeyRepository.countByUserIdAndActiveTrue(user.getId()) >= maxKeysPerUser) {
            throw new BadRequestException("Máximo " + maxKeysPerUser + " API keys activas por cuenta. Revoca una para crear otra");
        }
        String raw = ApiKeys.generate();
        ApiKey key = apiKeyRepository.save(ApiKey.builder()
                .user(user)
                .name(cleanName)
                .prefix(ApiKeys.displayPrefix(raw))
                .keyHash(ApiKeys.sha256(raw))
                .dailyLimit(defaultDailyLimit)
                .build());
        log.info("API key {} creada para {}", key.getPrefix(), user.getEmail());
        return new CreatedKeyDTO(key.getId(), key.getName(), raw, key.getPrefix(), key.getDailyLimit());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDTO> listForUser(User user) {
        return toDtos(apiKeyRepository.findByUserIdOrderByCreatedAtDesc(user.getId()), false);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDTO> listAll() {
        return toDtos(apiKeyRepository.findAllWithUser(), true);
    }

    @Transactional
    public void revoke(User user, UUID keyId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new PoiNotFoundException("API key no encontrada"));
        deactivate(key);
    }

    @Transactional
    public ApiKeyDTO update(UUID keyId, Boolean active, Integer dailyLimit) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new PoiNotFoundException("API key no encontrada"));
        if (dailyLimit != null) {
            if (dailyLimit < 1 || dailyLimit > 1_000_000) {
                throw new BadRequestException("daily_limit debe estar entre 1 y 1,000,000");
            }
            key.setDailyLimit(dailyLimit);
        }
        if (Boolean.FALSE.equals(active)) {
            deactivate(key);
        } else if (Boolean.TRUE.equals(active)) {
            key.setActive(true);
            key.setRevokedAt(null);
        }
        return toDtos(List.of(key), true).getFirst();
    }

    private void deactivate(ApiKey key) {
        key.setActive(false);
        if (key.getRevokedAt() == null) {
            key.setRevokedAt(LocalDateTime.now());
        }
        log.info("API key {} revocada", key.getPrefix());
    }

    /**
     * Valida la key y descuenta una petición de su cuota diaria.
     * Devuelve null si la key no existe, está revocada o su dueño está deshabilitado.
     */
    @Transactional
    public Consumption consume(String rawKey) {
        if (!ApiKeys.looksLikeKey(rawKey)) {
            return null;
        }
        ApiKey key = apiKeyRepository.findByKeyHashWithUser(ApiKeys.sha256(rawKey)).orElse(null);
        if (key == null || !Boolean.TRUE.equals(key.getActive()) || !Boolean.TRUE.equals(key.getUser().getEnabled())) {
            return null;
        }
        LocalDate day = today();
        int used = usageRepository.increment(key.getId(), day);
        apiKeyRepository.touch(key.getId(), LocalDateTime.now());
        int limit = key.getDailyLimit();
        long reset = day.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        return new Consumption(key.getId(),
                new FixedWindowRateLimiter.Result(used <= limit, limit, Math.max(0, limit - used), reset));
    }

    private List<ApiKeyDTO> toDtos(List<ApiKey> keys, boolean withOwner) {
        if (keys.isEmpty()) {
            return List.of();
        }
        LocalDate today = today();
        LocalDate from = today.minusDays(HISTORY_DAYS - 1L);
        Collection<UUID> ids = keys.stream().map(ApiKey::getId).toList();
        Map<UUID, Map<LocalDate, Integer>> usage = usageRepository.findByApiKeyIdInAndUsageDateGreaterThanEqual(ids, from)
                .stream()
                .collect(Collectors.groupingBy(ApiKeyUsage::getApiKeyId,
                        Collectors.toMap(ApiKeyUsage::getUsageDate, ApiKeyUsage::getRequests)));
        Function<ApiKey, ApiKeyDTO> mapper = k -> {
            Map<LocalDate, Integer> byDay = usage.getOrDefault(k.getId(), Map.of());
            List<DailyUsage> history = new ArrayList<>();
            for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
                history.add(new DailyUsage(d, byDay.getOrDefault(d, 0)));
            }
            return ApiKeyDTO.from(k, byDay.getOrDefault(today, 0), history,
                    withOwner ? k.getUser().getEmail() : null);
        };
        return keys.stream().map(mapper).toList();
    }
}
