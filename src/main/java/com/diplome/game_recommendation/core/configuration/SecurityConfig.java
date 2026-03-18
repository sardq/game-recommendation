package com.diplome.game_recommendation.core.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserAuthenticationEntryPoint userAuthenticationEntryPoint;
    private final UserAuthenticationProvider userAuthenticationProvider;
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String DISCIPLINE_CONTROLLER = "/api/disciplines/**";
    private static final String GROUP_CONTROLLER = "/api/groups/**";
    private static final String EXERCISE_CONTROLLER = "/api/exercises/**";
    private static final String GRADE_CONTROLLER = "/api/grades/**";

    public SecurityConfig(UserAuthenticationEntryPoint userAuthenticationEntryPoint,
            UserAuthenticationProvider userAuthenticationProvider) {
        this.userAuthenticationEntryPoint = userAuthenticationEntryPoint;
        this.userAuthenticationProvider = userAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(userAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthFilter(userAuthenticationProvider), BasicAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable()) // CSRF disabled because JWT is stateless NOSONAR
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/login", "/register", "/reset-password",
                                "/api/otp/**")
                        .permitAll()
                        .requestMatchers("/api/protocol/**")
                        .hasRole(TEACHER_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/users/**")
                        .hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.GET,
                                DISCIPLINE_CONTROLLER,
                                GROUP_CONTROLLER,
                                EXERCISE_CONTROLLER,
                                GRADE_CONTROLLER)
                        .hasAnyRole("STUDENT", TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.POST,
                                DISCIPLINE_CONTROLLER,
                                GROUP_CONTROLLER,
                                EXERCISE_CONTROLLER,
                                GRADE_CONTROLLER)
                        .hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.PUT,
                                DISCIPLINE_CONTROLLER,
                                GROUP_CONTROLLER,
                                EXERCISE_CONTROLLER,
                                GRADE_CONTROLLER)
                        .hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE,
                                DISCIPLINE_CONTROLLER,
                                GROUP_CONTROLLER,
                                EXERCISE_CONTROLLER,
                                GRADE_CONTROLLER)
                        .hasAnyRole(TEACHER_ROLE, ADMIN_ROLE)
                        .requestMatchers("/api/user/me",
                                "/api/users/me")
                        .authenticated()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
