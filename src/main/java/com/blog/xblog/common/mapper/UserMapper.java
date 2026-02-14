package com.blog.xblog.common.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.blog.xblog.user.dto.UserProfileResponse;
import com.blog.xblog.user.dto.UserResponse;
import com.blog.xblog.user.dto.UserProfileUpdateRequest;
import com.blog.xblog.user.entity.UserEntity;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toUserResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static List<UserResponse> toUserResponses(List<UserEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(UserMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    public static UserProfileResponse toUserProfileResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new UserProfileResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static void applyProfileUpdate(UserProfileUpdateRequest request, UserEntity entity) {
        if (request == null || entity == null) {
            return;
        }

        entity.setUsername(request.username());
        entity.setEmail(request.email());
    }
}
