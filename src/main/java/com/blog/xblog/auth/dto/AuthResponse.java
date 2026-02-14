package com.blog.xblog.auth.dto;

import com.blog.xblog.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
