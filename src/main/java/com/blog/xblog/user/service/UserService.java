package com.blog.xblog.user.service;

import java.util.Optional;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blog.xblog.common.exception.BadRequestException;
import com.blog.xblog.common.exception.NotFoundException;
import com.blog.xblog.common.mapper.UserMapper;
import com.blog.xblog.common.util.DateTimeUtil;
import com.blog.xblog.user.dto.UserProfileResponse;
import com.blog.xblog.user.dto.UserProfileUpdateRequest;
import com.blog.xblog.user.dto.UserResponse;
import com.blog.xblog.user.entity.UserEntity;
import com.blog.xblog.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public UserService(UserRepository userRepository, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users", key = "#id")
    public UserResponse getById(Long id) {
        return UserMapper.toUserResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userProfiles", key = "#id")
    public UserProfileResponse getProfileById(Long id) {
        return UserMapper.toUserProfileResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long id, UserProfileUpdateRequest request) {
        UserEntity user = findUserOrThrow(id);

        userRepository.findByUsername(request.username())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> { throw new BadRequestException("Username is already taken"); });

        userRepository.findByEmail(request.email())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> { throw new BadRequestException("Email is already in use"); });

        UserMapper.applyProfileUpdate(request, user);
        user.setUpdatedAt(DateTimeUtil.now());
        user = userRepository.save(user);

        evictUserCaches(id);

        return UserMapper.toUserProfileResponse(user);
    }

    private void evictUserCaches(Long id) {
        try {
            var users = cacheManager.getCache("users");
            if (users != null) users.evict(id);
            var profiles = cacheManager.getCache("userProfiles");
            if (profiles != null) profiles.evict(id);
        } catch (Exception ignored) {
        }
    }

    private UserEntity findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id " + id));
    }
}
