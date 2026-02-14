package com.blog.xblog.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.xblog.common.dto.ApiResponse;
import com.blog.xblog.common.security.CustomUserDetails;
import com.blog.xblog.user.dto.UserProfileResponse;
import com.blog.xblog.user.dto.UserProfileUpdateRequest;
import com.blog.xblog.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Current user", description = "Profile of the authenticated user (JWT required)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get my profile", description = "Returns the profile of the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal) {
        UserProfileResponse profile = userService.getProfileById(principal.getId());
        ApiResponse<UserProfileResponse> body = ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .message("Current user profile")
                .data(profile)
                .build();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Update my profile", description = "Update username/email of the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UserProfileResponse profile = userService.updateProfile(principal.getId(), request);
        ApiResponse<UserProfileResponse> body = ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .message("Profile updated")
                .data(profile)
                .build();
        return ResponseEntity.ok(body);
    }
}
