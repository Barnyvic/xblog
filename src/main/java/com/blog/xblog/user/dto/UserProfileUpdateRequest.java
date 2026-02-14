package com.blog.xblog.user.dto;

import jakarta.validation.constraints.Email;

public record UserProfileUpdateRequest(
         String username,
         @Email String email
) {}
