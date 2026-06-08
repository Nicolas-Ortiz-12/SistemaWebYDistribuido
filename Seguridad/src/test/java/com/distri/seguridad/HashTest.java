package com.distri.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashTest {
    @Test
    public void generateHash() {
        System.out.println("HASH_ADMIN: " + new BCryptPasswordEncoder().encode("admin"));
    }
}
