package org.collapseloader.atlas.config;

import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.util.passwords.HybridPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOriginPatterns(java.util.List.of(
                            "http://localhost:1420",
                            "http://127.0.0.1:1420",
                            "tauri://localhost",
                            "https://tauri.localhost",
                            "http://tauri.localhost",
                            "http://localhost:5173",
                            "https://collapseloader.org",
                            "https://calypso.collapseloader.org",
                            "http://atlas.collapseloader.org",
                            "https://atlas.collapseloader.org"));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setExposedHeaders(java.util.List.of("Authorization"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()


                        .requestMatchers("/api/v1/agent/download/**", "/api/v1/overlay/download/**").permitAll()
                        .requestMatchers("/api/v1/agent-overlay/**").permitAll()

                        .requestMatchers("/api/v1/loader/**").permitAll()
                        .requestMatchers("/api/v1/statistics").permitAll()

                        .requestMatchers("/api/v1/clients/**").permitAll()
                        .requestMatchers("/api/v1/fabric-clients/**").permitAll()
                        .requestMatchers("/api/v1/forge-clients/**").permitAll()

                        .requestMatchers("/api/v1/news/**").permitAll()
                        .requestMatchers("/api/v1/presets/**").permitAll()

                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getServletPath().startsWith("/api/")) {
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized");
                            } else {
                                response.sendRedirect("/login");
                            }
                        }));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            HybridPasswordEncoder hybridPasswordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(hybridPasswordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public HybridPasswordEncoder hybridPasswordEncoder() {
        return new HybridPasswordEncoder();
    }

    @Bean
    public PasswordEncoder passwordEncoder(HybridPasswordEncoder hybridPasswordEncoder) {
        return hybridPasswordEncoder;
    }

}
