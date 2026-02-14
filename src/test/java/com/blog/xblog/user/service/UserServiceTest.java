package com.blog.xblog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import com.blog.xblog.common.exception.BadRequestException;
import com.blog.xblog.common.exception.NotFoundException;
import com.blog.xblog.user.dto.UserProfileResponse;
import com.blog.xblog.user.dto.UserProfileUpdateRequest;
import com.blog.xblog.user.entity.UserEntity;
import com.blog.xblog.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    private UserService userService;

    private static final Instant NOW = Instant.parse("2026-02-14T12:00:00Z");

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, cacheManager);
    }

    @Nested
    @DisplayName("getProfileById")
    class GetProfileById {

        @Test
        @DisplayName("when user exists returns profile")
        void whenUserExists_returnsProfile() {
            UserEntity user = userEntity(1L, "alice", "alice@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserProfileResponse result = userService.getProfileById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.username()).isEqualTo("alice");
            assertThat(result.email()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("when user does not exist throws NotFoundException")
        void whenUserNotFound_throwsNotFoundException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfileById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found with id 999");
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("when username already taken by another user throws BadRequestException")
        void whenUsernameTaken_throwsBadRequest() {
            UserEntity currentUser = userEntity(1L, "alice", "alice@example.com");
            UserEntity otherUser = userEntity(2L, "bob", "bob@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("bob")).thenReturn(Optional.of(otherUser));

            UserProfileUpdateRequest request = new UserProfileUpdateRequest("bob", "alice@example.com");

            assertThatThrownBy(() -> userService.updateProfile(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username is already taken");
        }

        @Test
        @DisplayName("when email already taken by another user throws BadRequestException")
        void whenEmailTaken_throwsBadRequest() {
            UserEntity currentUser = userEntity(1L, "alice", "alice@example.com");
            UserEntity otherUser = userEntity(2L, "bob", "bob@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(currentUser));
            when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));

            UserProfileUpdateRequest request = new UserProfileUpdateRequest("alice", "bob@example.com");

            assertThatThrownBy(() -> userService.updateProfile(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email is already in use");
        }
    }

    private static UserEntity userEntity(Long id, String username, String email) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .email(email)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
