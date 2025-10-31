package com.cybersecurity.sechamp2025.config;

import com.cybersecurity.sechamp2025.utils.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .authorizeHttpRequests(auth -> auth
                // API endpoints first (more specific)
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/demo/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/books/**").permitAll()
                .requestMatchers("/api/users/*/credits").permitAll()
                .requestMatchers("/api/users/*").permitAll()  // Allow public access to fetch user by userid
                .requestMatchers("/api/users").authenticated()
                .requestMatchers("/api/profile/**").authenticated()
                .requestMatchers("/api/user/**").authenticated()
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/purchase/**").authenticated()
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/favicon.ico", "/favicon.svg").permitAll()
                .requestMatchers("/bak/**").permitAll()
                // Page routes
                .requestMatchers("/", "/login", "/register", "/books", "/bookstore-location", "/help", "/advanced_search", "/advanced_search/**").permitAll()
                .requestMatchers("/users", "/comments", "/profile", "/admin").permitAll()
                .requestMatchers("/purchase/**").permitAll()
                .requestMatchers("/cart", "/checkout").permitAll()
                .requestMatchers("/voucher", "/voucher/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
