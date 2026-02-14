package com.blog.xblog.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.xblog.auth.dto.AuthResponse;
import com.blog.xblog.auth.dto.LoginRequest;
import com.blog.xblog.auth.dto.RegistrationRequest;
import com.blog.xblog.auth.service.AuthService;
import com.blog.xblog.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Auth", description = "Login and registration (no JWT required)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register", description = "Create a new user and return JWT + user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegistrationRequest request) {
        AuthResponse auth = authService.register(request);
        ApiResponse<AuthResponse> body = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("User registered successfully")
                .data(auth)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Login", description = "Authenticate and get JWT + user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        ApiResponse<AuthResponse> body = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(auth)
                .build();
        return ResponseEntity.ok(body);
    }
}
