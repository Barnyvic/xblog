package com.blog.xblog.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blog.xblog.auth.dto.AuthResponse;
import com.blog.xblog.auth.dto.LoginRequest;
import com.blog.xblog.auth.dto.RegistrationRequest;
import com.blog.xblog.common.exception.BadRequestException;
import com.blog.xblog.common.mapper.UserMapper;
import com.blog.xblog.common.security.JwtTokenProvider;
import com.blog.xblog.common.util.DateTimeUtil;
import com.blog.xblog.user.dto.UserResponse;
import com.blog.xblog.user.entity.UserEntity;
import com.blog.xblog.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegistrationRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.findAll().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(request.email()))) {
            throw new BadRequestException("Email is already in use");
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .createdAt(DateTimeUtil.now())
                .updatedAt(DateTimeUtil.now())
                .build();

        user = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        UserResponse userResponse = UserMapper.toUserResponse(user);

        return new AuthResponse(token, "Bearer", 0L, userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = jwtTokenProvider.generateToken(authentication);

        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        UserResponse userResponse = UserMapper.toUserResponse(user);

        return new AuthResponse(token, "Bearer", 0L, userResponse);
    }
}
