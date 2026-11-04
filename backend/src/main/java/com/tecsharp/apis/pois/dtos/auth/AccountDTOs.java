package com.tecsharp.apis.pois.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tecsharp.apis.pois.models.ApiKey;
import com.tecsharp.apis.pois.models.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTOs de cuenta, API keys y administración. */
public final class AccountDTOs {

    private AccountDTOs() {
    }

    public record RegisterRequest(String name, String email, String password) {}

    public record LoginRequest(String email, String password) {}

    public record UserDTO(UUID id, String name, String email, String role, Boolean enabled,
                          @JsonProperty("created_at") LocalDateTime createdAt) {
        public static UserDTO from(User u) {
            return new UserDTO(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getEnabled(), u.getCreatedAt());
        }
    }

    public record AuthResponse(String token, UserDTO user) {}

    public record CreateKeyRequest(String name) {}

    public record DailyUsage(LocalDate date, int requests) {}

    public record ApiKeyDTO(UUID id, String name, String prefix, Boolean active,
                            @JsonProperty("daily_limit") Integer dailyLimit,
                            @JsonProperty("used_today") int usedToday,
                            @JsonProperty("created_at") LocalDateTime createdAt,
                            @JsonProperty("last_used_at") LocalDateTime lastUsedAt,
                            @JsonProperty("revoked_at") LocalDateTime revokedAt,
                            List<DailyUsage> history,
                            @JsonProperty("owner_email") String ownerEmail) {
        public static ApiKeyDTO from(ApiKey k, int usedToday, List<DailyUsage> history, String ownerEmail) {
            return new ApiKeyDTO(k.getId(), k.getName(), k.getPrefix(), k.getActive(), k.getDailyLimit(), usedToday,
                    k.getCreatedAt(), k.getLastUsedAt(), k.getRevokedAt(), history, ownerEmail);
        }
    }

    /** Respuesta de creación: la única vez que se devuelve la key completa. */
    public record CreatedKeyDTO(UUID id, String name, String key, String prefix,
                                @JsonProperty("daily_limit") Integer dailyLimit) {}

    public record UpdateKeyRequest(Boolean active, @JsonProperty("daily_limit") Integer dailyLimit) {}

    public record UpdateUserRequest(Boolean enabled) {}

    public record AdminUserDTO(UserDTO user, @JsonProperty("active_keys") long activeKeys) {}

    public record StatsDTO(long users, @JsonProperty("active_keys") long activeKeys, long pois,
                           @JsonProperty("requests_today") long requestsToday) {}
}
