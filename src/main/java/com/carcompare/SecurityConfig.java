package com.carcompare;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthTokenRepository tokens, AppUserRepository users) throws Exception {
        http.csrf(c -> c.disable())                       // stateless token API, no cookies -> no CSRF risk
            .cors(Customizer.withDefaults())              // uses CorsConfig
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(a -> a
                    .requestMatchers("/api/favorites/**", "/api/auth/me", "/api/auth/logout").authenticated()
                    .anyRequest().permitAll())            // browsing, compare, recommend, static files stay public
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new TokenAuthFilter(tokens, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
