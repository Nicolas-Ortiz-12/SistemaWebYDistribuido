package com.distri.seguridad.security;

import com.distri.seguridad.domain.Role;
import com.distri.seguridad.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

// Roles de un usuario activo, cacheados en Redis para no ir a la BD en cada request (JwtAuthFilter).
// Se guarda solo el texto "ADMIN,VENDEDOR" (sin hash de contraseña ni objetos serializados).
@Service
@RequiredArgsConstructor
public class RolesCache {
    public static final String CACHE = "usuarios-roles";

    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE, key = "#username")
    public String rolesOf(String username) {
        var u = userRepo.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return u.getRoles().stream().map(Role::getName).collect(Collectors.joining(","));
    }
}
