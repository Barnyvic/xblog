package com.blog.xblog.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        Instant createdAt,
        Instant updatedAt
) {}
