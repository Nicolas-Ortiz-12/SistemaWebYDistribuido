package com.distri.proyectoVenta.security;



import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // públicos del micro (ajustá lo que necesites)
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // EJEMPLOS de reglas:
                        .requestMatchers(HttpMethod.POST, "/ventas/**").hasAnyRole("ADMIN","VENDEDOR")
                        .requestMatchers(HttpMethod.PUT,  "/ventas/**").hasAnyRole("ADMIN","VENDEDOR")
                        .requestMatchers(HttpMethod.DELETE,"/ventas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/ventas/**").hasAnyRole("ADMIN","VENDEDOR","USER")

                        // categorías/productos si viven en este micro:
                        // .requestMatchers(HttpMethod.GET, "/productos/**").hasAnyRole("ADMIN","USER","VENDEDOR")
                        // .requestMatchers(HttpMethod.POST,"/productos/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
