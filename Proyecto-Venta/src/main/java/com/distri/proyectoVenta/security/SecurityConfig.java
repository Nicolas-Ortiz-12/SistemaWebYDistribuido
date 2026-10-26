package com.distri.proyectoVenta.security;



import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;
    private final RestAuthEntryPoint entryPoint;
    private final RestAccessDeniedHandler deniedHandler;

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(reg -> reg
                        // públicos del micro
                        .requestMatchers("/error", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // administración de usuarios y roles: solo ADMIN
                        .requestMatchers("/usuarios/**", "/roles/**").hasRole("ADMIN")

                        // ventas: anular solo ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/ventas/**").hasRole("ADMIN")
                        .requestMatchers("/ventas/**").hasAnyRole("ADMIN","VENDEDOR")

                        // proveedores: consultar ADMIN/VENDEDOR, modificar solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/proveedores/**").hasAnyRole("ADMIN","VENDEDOR")
                        .requestMatchers("/proveedores/**").hasRole("ADMIN")

                        .requestMatchers("/categorias/**", "/clientes/**", "/productos/**").hasAnyRole("ADMIN","VENDEDOR")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
