package com.distri.seguridad.security;

import com.distri.seguridad.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var u = userRepo.findByUsernameAndEnabledTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities = u.getRoles().stream()
                // IMPORTANTE: si usás hasRole('ADMIN'), hay que prefijar con "ROLE_"
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .toList();

        System.err.println("DB HASH: " + u.getPasswordHash());
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),   // el campo correcto
                authorities
        );
    }
}
