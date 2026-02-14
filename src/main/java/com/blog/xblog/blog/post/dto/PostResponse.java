package com.blog.xblog.blog.post.dto;

import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String slug,
        String content,
        Long authorId,
        String authorUsername,
        Instant createdAt,
        Instant updatedAt
) {}

