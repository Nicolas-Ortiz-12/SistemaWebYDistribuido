package com.distri.seguridad.service;


import com.distri.seguridad.domain.Role;
import com.distri.seguridad.domain.UserAccount;
import com.distri.seguridad.repository.RoleRepo;
import com.distri.seguridad.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder encoder;

    @Override
    public Optional<UserAccount> findActiveByUsername(String username) {
        return userRepo.findByUsernameAndEnabledTrue(username);
    }

    @Override
    public UserAccount create(String username, String rawPassword, String fullName, Set<String> roles) {
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword)); // BCrypt
        u.setFullName(fullName);
        for (String rn : roles) {
            Role r = roleRepo.findByName(rn).orElseGet(() -> {
                Role nr = new Role(); nr.setName(rn);
                return roleRepo.save(nr);
            });
            u.getRoles().add(r);
        }
        return userRepo.save(u);
    }
}