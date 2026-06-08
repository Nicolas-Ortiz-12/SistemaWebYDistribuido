package com.distri.seguridad.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

// domain/UserAccount.java
@Entity
@Table(name="users")
@Getter
@Setter @NoArgsConstructor
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=80)
    private String username;

    @Column(nullable=false, length=255)
    private String passwordHash; // BCrypt

    @Column(nullable=false, length=200)
    private String fullName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="user_roles",
            joinColumns=@JoinColumn(name="user_id"),
            inverseJoinColumns=@JoinColumn(name="role_id"))
    private Set<Role> roles = new HashSet<>();

    private boolean enabled = true;
}
