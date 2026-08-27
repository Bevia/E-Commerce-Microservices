package org.example.paymentmicroservice.security;

import org.example.paymentmicroservice.jwt.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebSecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/h2-console/**").permitAll() // Allow H2 console for dev
                                .requestMatchers("/actuator/health/**").permitAll() // Allow platform health probes
                                .anyRequest().authenticated() // All other requests require valid JWT
                );

        // For H2-Console to work with Spring Security
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        // Add JWT filter before Spring Security's authentication filter
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
