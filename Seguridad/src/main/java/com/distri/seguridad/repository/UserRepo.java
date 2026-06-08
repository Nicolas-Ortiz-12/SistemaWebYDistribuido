package com.distri.seguridad.repository;


import com.distri.seguridad.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameAndEnabledTrue(String username);
}