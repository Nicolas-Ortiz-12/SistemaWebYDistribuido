package com.distri.seguridad.web;


import com.distri.seguridad.domain.UserAccount;
import com.distri.seguridad.repository.RoleRepo;
import com.distri.seguridad.repository.UserRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder encoder;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody UserDTO body) {
        if (userRepo.findByUsernameAndEnabledTrue(body.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        var user = new UserAccount();
        user.setUsername(body.username());
        user.setFullName(body.fullName());
        user.setPasswordHash(encoder.encode(body.password()));
        user.setEnabled(true);

        // asignar roles
        var role = roleRepo.findByName(body.role()).orElseThrow();
        user.getRoles().add(role);

        userRepo.save(user);
        return ResponseEntity.ok("User created");
    }

    // ✅ hacelo público y estático para que Spring pueda accederlo
    public static record UserDTO(
            @jakarta.validation.constraints.NotBlank String username,
            @jakarta.validation.constraints.NotBlank String fullName,
            @jakarta.validation.constraints.NotBlank String password,
            @jakarta.validation.constraints.NotBlank String role
    ) {}
}