package org.example.backendweride.platform.iam.infrastructure.auth.configuration;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.example.backendweride.platform.iam.infrastructure.auth.pipeline.BearerAuthRequestFilter;
import org.example.backendweride.platform.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import org.example.backendweride.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration class for web security using Spring Security.
 *
 * @summary This class sets up security filters, authentication providers, and CORS configurations.
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    private final UserDetailsService userDetailsService;
    private final BearerTokenService tokenService;
    private final BCryptHashingService hashingService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Creates the web security configuration with its authentication dependencies.
     */
    public WebSecurityConfig(
            @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
            BearerTokenService tokenService,
            BCryptHashingService hashingService,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * Creates the bearer authentication request filter.
     *
     * @return configured bearer authentication filter
     */
    @Bean
    public BearerAuthRequestFilter authRequestFilter() {
        return new BearerAuthRequestFilter(tokenService, userDetailsService);
    }

    /**
     * Exposes the application authentication manager.
     *
     * @param authConfig Spring Security authentication configuration
     * @return configured authentication manager
     * @throws Exception if the authentication manager cannot be obtained
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Creates the DAO authentication provider.
     *
     * @return configured DAO authentication provider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider(hashingService);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    /**
     * Exposes the password encoder used by the application.
     *
     * @return configured password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() { return hashingService;}

    /**
     * Creates the CORS configuration source.
     *
     * @return configured CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://frontend-we-ride.vercel.app",
                "https://backend-weride-2uvw.onrender.com"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * Configures the application's HTTP security filter chain.
     *
     * @param http HTTP security configuration
     * @return configured security filter chain
     * @throws Exception if the security filter chain cannot be configured
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(customizer ->
                        customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(
                                "/health",
                                "/api/v1/authentication/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated());
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

