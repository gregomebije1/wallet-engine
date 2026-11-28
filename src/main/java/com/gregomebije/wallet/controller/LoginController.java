package com.gregomebije.wallet.controller; 

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;

import com.gregomebije.wallet.dto.Models.*;
import com.gregomebije.wallet.service.AuthenticationService;

@RestController
@RequestMapping("/api/wallet/v1")
public class LoginController {
	 private final AuthenticationService authenticationService;


	public LoginController(
            AuthenticationService authenticationService) {

		this.authenticationService = authenticationService;
	}

    @GetMapping("/setup")
    ResponseEntity<?> setup() {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("status", "success", "data", authenticationService.setup()));
    }

    @PostMapping("/register")
    ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request) {
         return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("status", "success", "data", authenticationService.register(request)));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody WalletLoginRequest request) {
         return ResponseEntity.ok(authenticationService.login(request));
    }

}
