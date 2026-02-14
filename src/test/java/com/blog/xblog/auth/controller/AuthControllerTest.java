package com.blog.xblog.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import com.blog.xblog.auth.dto.AuthResponse;
import com.blog.xblog.auth.dto.LoginRequest;
import com.blog.xblog.auth.dto.RegistrationRequest;
import com.blog.xblog.auth.service.AuthService;
import com.blog.xblog.common.exception.GlobalExceptionHandler;
import com.blog.xblog.user.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("when credentials valid returns 200 and token")
        void whenValid_returns200AndToken() throws Exception {
            UserResponse user = new UserResponse(1L, "alice", "alice@example.com", null, null);
            AuthResponse auth = new AuthResponse("jwt-token", "Bearer", 86400L, user);

            when(authService.login(any(LoginRequest.class))).thenReturn(auth);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                    .andExpect(jsonPath("$.data.user.username").value("alice"));
        }

        @Test
        @DisplayName("when credentials invalid returns 401")
        void whenInvalid_returns401() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signup")
    class Signup {

        @Test
        @DisplayName("when request valid returns 201 and token")
        void whenValid_returns201AndToken() throws Exception {
            UserResponse user = new UserResponse(1L, "alice", "alice@example.com", null, null);
            AuthResponse auth = new AuthResponse("jwt-token", "Bearer", 86400L, user);

            when(authService.register(any(RegistrationRequest.class))).thenReturn(auth);

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"password\",\"email\":\"alice@example.com\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
        }
    }
}
