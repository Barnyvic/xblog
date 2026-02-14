package com.blog.xblog.common.mapper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.blog.xblog.blog.entity.BlogEntity;
import com.blog.xblog.blog.post.dto.PostCreateRequest;
import com.blog.xblog.blog.post.dto.PostResponse;
import com.blog.xblog.blog.post.dto.PostUpdateRequest;
import com.blog.xblog.user.entity.UserEntity;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toPostResponse(BlogEntity entity) {
        if (entity == null) {
            return null;
        }

        UserEntity author = entity.getAuthor();

        return new PostResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getContent(),
                author != null ? author.getId() : null,
                author != null ? author.getUsername() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<PostResponse> toPostResponses(List<BlogEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(PostMapper::toPostResponse)
                .collect(Collectors.toList());
    }

    public static BlogEntity toBlogEntity(PostCreateRequest request,
                                          UserEntity author,
                                          String slug,
                                          Instant createdAt,
                                          Instant updatedAt) {
        if (request == null || author == null) {
            return null;
        }

        return BlogEntity.builder()
                .title(request.title())
                .slug(slug)
                .content(request.content())
                .author(author)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static void applyUpdate(PostUpdateRequest request, BlogEntity entity, Instant updatedAt) {
        if (request == null || entity == null) {
            return;
        }

        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setUpdatedAt(updatedAt);
    }
}
