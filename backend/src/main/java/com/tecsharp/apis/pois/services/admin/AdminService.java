package com.tecsharp.apis.pois.services.admin;

import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.AdminUserDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.StatsDTO;
import com.tecsharp.apis.pois.dtos.auth.AccountDTOs.UserDTO;
import com.tecsharp.apis.pois.exceptions.BadRequestException;
import com.tecsharp.apis.pois.exceptions.UserNotFoundException;
import com.tecsharp.apis.pois.models.User;
import com.tecsharp.apis.pois.repositories.ApiKeyRepository;
import com.tecsharp.apis.pois.repositories.ApiKeyUsageRepository;
import com.tecsharp.apis.pois.repositories.PoiRepository;
import com.tecsharp.apis.pois.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageRepository usageRepository;
    private final PoiRepository poiRepository;

    @Transactional(readOnly = true)
    public StatsDTO stats() {
        return new StatsDTO(userRepository.count(), apiKeyRepository.countByActiveTrue(), poiRepository.count(),
                usageRepository.sumByDay(LocalDate.now(ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public List<AdminUserDTO> users() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new AdminUserDTO(UserDTO.from(u), apiKeyRepository.countByUserIdAndActiveTrue(u.getId())))
                .toList();
    }

    @Transactional
    public UserDTO setEnabled(User admin, UUID userId, boolean enabled) {
        if (admin.getId().equals(userId)) {
            throw new BadRequestException("No puedes deshabilitar tu propia cuenta");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        user.setEnabled(enabled);
        return UserDTO.from(user);
    }
}
