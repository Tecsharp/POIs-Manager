package com.tecsharp.apis.pois.config;

import com.tecsharp.apis.pois.repositories.UserRepository;
import com.tecsharp.apis.pois.security.ApiKeyFilter;
import com.tecsharp.apis.pois.security.AuthRateLimitFilter;
import com.tecsharp.apis.pois.security.FixedWindowRateLimiter;
import com.tecsharp.apis.pois.security.JwtAuthFilter;
import com.tecsharp.apis.pois.security.JwtService;
import com.tecsharp.apis.pois.services.apikey.ApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserRepository userRepository,
                                                   ApiKeyService apiKeyService,
                                                   @Value("${pois.anonymous.requests-per-minute:60}") int anonPerMinute) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> ApiKeyFilter.writeError(res, 401, "Inicia sesión para continuar"))
                        .accessDeniedHandler((req, res, e) -> ApiKeyFilter.writeError(res, 403, "No tienes permisos para realizar esta acción")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/pois/route").permitAll()
                        .requestMatchers(HttpMethod.GET, "/pois", "/pois/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/pois").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/pois/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                // Una instancia por filtro creada aquí (sin @Component) para que no se registre dos veces.
                .addFilterBefore(new AuthRateLimitFilter(new FixedWindowRateLimiter(10, 60_000)),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ApiKeyFilter(apiKeyService, new FixedWindowRateLimiter(anonPerMinute, 60_000)),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** La API pública se puede consumir desde cualquier origen con API key (sin cookies). */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "X-API-Key", "Content-Type"));
        config.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "X-RateLimit-Scope", "Retry-After"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/pois/**", config);
        source.registerCorsConfiguration("/pois", config);
        return source;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
