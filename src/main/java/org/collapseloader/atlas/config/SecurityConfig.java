package org.collapseloader.atlas.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.collapseloader.atlas.domain.users.passwords.HybridPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Lazy
    private final JwtAuthenticationFilter jwtAuthFilter;
    @Lazy
    private final RateLimitFilter rateLimitFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain prometheusSecurityFilterChain(
            HttpSecurity http,
            @Value("${atlas.monitoring.prometheus.username}") String prometheusUsername,
            @Value("${atlas.monitoring.prometheus.password}") String prometheusPassword,
            PasswordEncoder passwordEncoder) {
        UserDetailsService prometheusScrapeUserDetailsService = new InMemoryUserDetailsManager(User.withUsername(prometheusUsername)
                .password(passwordEncoder.encode(prometheusPassword))
                .roles("PROMETHEUS")
                .build());

        DaoAuthenticationProvider prometheusAuthenticationProvider = new DaoAuthenticationProvider(prometheusScrapeUserDetailsService);
        prometheusAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        http
                .securityMatcher("/actuator/prometheus")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(prometheusAuthenticationProvider)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("PROMETHEUS"))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of(
                            "http://localhost:1420",
                            "http://127.0.0.1:1420",
                            "tauri://localhost",
                            "https://tauri.localhost",
                            "http://tauri.localhost",
                            "http://localhost:5173",
                            "https://collapseloader.org",
                            "https://calypso.collapseloader.org",
                            "http://atlas.collapseloader.org",
                            "https://atlas.collapseloader.org",
                            "https://proxy.collapseloader.org"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("Authorization"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers("/api/v1/agent/download/**", "/api/v1/overlay/download/**").permitAll()
                        .requestMatchers("/api/v1/agent-overlay/**").permitAll()

                        .requestMatchers("/api/v1/loader/**").permitAll()
                        .requestMatchers("/api/v1/statistics").permitAll()
                        .requestMatchers("/api/v1/analytics/**").permitAll()

                        .requestMatchers("/api/v1/clients/**").permitAll()
                        .requestMatchers("/api/v1/fabric-clients/**").permitAll()
                        .requestMatchers("/api/v1/forge-clients/**").permitAll()

                        .requestMatchers("/api/v1/news/**").permitAll()
                        .requestMatchers("/api/v1/presets/**").permitAll()
                        .requestMatchers("/api/v1/crash-logs").permitAll()

                        .requestMatchers("/api/v1/auth/setPassword", "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, RateLimitFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getServletPath().startsWith("/api/")) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized");
                            } else {
                                response.sendRedirect("/login");
                            }
                        }));

        return http.build();
    }

    // Prometheus provider is created locally in the prometheusSecurityFilterChain to avoid
    // registering a global AuthenticationProvider/UserDetailsService bean which causes Spring Boot
    // to skip UserDetailsService auto-configuration and emit a warning about multiple
    // UserDetailsService beans.

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
