package com.blog.xblog.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserProfileUpdateRequest(
        @NotBlank String username,
        @NotBlank @Email String email
) {}
