package com.gregomebije.wallet.config;

import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.gregomebije.wallet.config.UserDetailsServiceImpl;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
	private final AuthenticationFilter authenticationFilter;
	private final AuthEntryPoint exceptionHandler;

    private static final String[] SWAGGER_PATHS = {"/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**"};


	public SecurityConfig(UserDetailsServiceImpl userDetailsService, AuthenticationFilter authenticationFilter,
			AuthEntryPoint exceptionHandler) {
		this.userDetailsService = userDetailsService;
		this.authenticationFilter = authenticationFilter;
		this.exceptionHandler = exceptionHandler;
	}

	/*
    @0 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST APIs
            .csrf(AbstractHttpConfigurer::disable)
            // Disable default HTTP Basic and Form Login prompts
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            // Use stateless sessions (no JSESSIONID cookies)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Configure endpoint permissions
            .authorizeHttpRequests(auth -> auth
                // Temporarily permit all endpoints while testing (restrict later)
                .anyRequest().permitAll()
            );

        return http.build();
    }
    */
    @Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {	
		http.csrf((csrf) -> csrf.disable()).cors(withDefaults())
				.sessionManagement(
						(sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
						.requestMatchers("/api/wallet/v1/setup").permitAll()
                        .requestMatchers("/api/wallet/v1/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/wallet/v1/login").permitAll()
						.requestMatchers(SWAGGER_PATHS).permitAll()
                        .anyRequest().authenticated())
				.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling((exceptionHandling) -> exceptionHandling.authenticationEntryPoint(exceptionHandler));

		return http.build();
	}
    

    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
	}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean 
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList("*"));
		config.setAllowedMethods(Arrays.asList("*"));
		config.setAllowedHeaders(Arrays.asList("*"));
		config.setAllowCredentials(false);
		config.applyPermitDefaultValues();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
