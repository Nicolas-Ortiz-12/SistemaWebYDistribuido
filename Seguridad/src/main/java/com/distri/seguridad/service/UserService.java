package com.distri.seguridad.service;

import com.distri.seguridad.domain.UserAccount;

import java.util.Optional;
import java.util.Set;

public interface UserService {
    Optional<UserAccount> findActiveByUsername(String username);
    UserAccount create(String username, String rawPassword, String fullName, Set<String> roles);
    // otros métodos (cambiar clave, asignar roles, etc.)
}
