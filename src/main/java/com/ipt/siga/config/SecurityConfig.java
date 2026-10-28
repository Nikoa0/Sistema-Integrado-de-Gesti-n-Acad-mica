package com.ipt.siga.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/api/health", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                        // Teacher: grades and section rosters
                        .requestMatchers(HttpMethod.PUT, "/api/enrollments/*/grade").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/sections/*/enrollments")
                        .hasAnyRole("ADMIN", "TEACHER")

                        // Student: offer and enrollments
                        .requestMatchers(HttpMethod.GET, "/api/students/*/offer")
                        .hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/students/*/enrollments").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/students/*/enrollments")
                        .hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/students/*")
                        .hasAnyRole("ADMIN", "STUDENT")

                        // Any authenticated user can read the academic catalog
                        .requestMatchers(HttpMethod.GET, "/api/careers/**", "/api/study-plans/**",
                                "/api/subjects/**", "/api/academic-periods", "/api/academic-periods/*",
                                "/api/academic-periods/*/sections", "/api/sections/*").authenticated()

                        // Everything else is administration
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    // Keycloak puts the roles in realm_access.roles; Spring expects authorities like ROLE_ADMIN
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.<GrantedAuthority>of();
            }
            return roles.stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return converter;
    }
}