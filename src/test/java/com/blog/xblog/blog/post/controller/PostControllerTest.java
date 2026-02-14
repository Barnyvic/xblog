package com.blog.xblog.blog.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.blog.xblog.blog.post.dto.PostCreateRequest;
import com.blog.xblog.blog.post.dto.PostResponse;
import com.blog.xblog.blog.post.dto.PostUpdateRequest;
import com.blog.xblog.blog.post.service.PostService;
import com.blog.xblog.common.exception.GlobalExceptionHandler;
import com.blog.xblog.common.exception.NotFoundException;
import com.blog.xblog.common.security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    private static final Instant NOW = Instant.parse("2026-02-14T12:00:00Z");
    private static final PostResponse SAMPLE_POST = new PostResponse(
            1L, "My Title", "my-title", "Content here", 10L, "alice",
            null, NOW, NOW);
    private static final PostResponse SAMPLE_POST_WITH_IMAGE = new PostResponse(
            1L, "My Title", "my-title", "Content here", 10L, "alice",
            "/api/posts/1/image", NOW, NOW);
    private static final long PRINCIPAL_ID = 1L;
    private static final UserDetails PRINCIPAL = new CustomUserDetails(PRINCIPAL_ID, "alice", "password");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        PostController controller = new PostController(postService);
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withPrincipal() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(PRINCIPAL, null, PRINCIPAL.getAuthorities()));
            return request;
        };
    }


    @Nested
    @DisplayName("GET /api/posts (list)")
    class ListPosts {

        @Test
        @DisplayName("returns 200 and list of posts when no auth")
        void returns200AndListWithoutAuth() throws Exception {
            when(postService.listPosts()).thenReturn(List.of(SAMPLE_POST));

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Posts list"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("My Title"))
                    .andExpect(jsonPath("$.data[0].slug").value("my-title"))
                    .andExpect(jsonPath("$.data[0].authorUsername").value("alice"));
        }

        @Test
        @DisplayName("returns 200 and empty list when no posts")
        void returns200AndEmptyList() throws Exception {
            when(postService.listPosts()).thenReturn(List.of());

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{id} (single post)")
    class GetPost {

        @Test
        @DisplayName("returns 200 and post when id exists")
        void returns200WhenExists() throws Exception {
            when(postService.getPost(1L)).thenReturn(SAMPLE_POST);

            mockMvc.perform(get("/api/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("My Title"))
                    .andExpect(jsonPath("$.data.imageUrl").isEmpty());
        }

        @Test
        @DisplayName("returns 200 with imageUrl when post has image")
        void returns200WithImageUrl() throws Exception {
            when(postService.getPost(1L)).thenReturn(SAMPLE_POST_WITH_IMAGE);

            mockMvc.perform(get("/api/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.imageUrl").value("/api/posts/1/image"));
        }

        @Test
        @DisplayName("returns 404 when post not found")
        void returns404WhenNotFound() throws Exception {
            when(postService.getPost(999L)).thenThrow(new NotFoundException("Post not found with id 999"));

            mockMvc.perform(get("/api/posts/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Post not found with id 999"));
        }
    }

    @Nested
    @DisplayName("GET /api/posts/{id}/image")
    class GetPostImage {

        @Test
        @DisplayName("returns 200 with image and Content-Type when image exists")
        void returns200WithImage() throws Exception {
            Resource resource = new ByteArrayResource("image-bytes".getBytes()) {
                @Override
                public String getFilename() {
                    return "x.jpg";
                }
            };
            when(postService.getPostImage(1L)).thenReturn(resource);

            mockMvc.perform(get("/api/posts/1/image"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE))
                    .andExpect(header().string("Cache-Control", "max-age=86400"))
                    .andExpect(content().bytes("image-bytes".getBytes()));
        }

        @Test
        @DisplayName("returns 200 with PNG Content-Type when filename is png")
        void returns200WithPngContentType() throws Exception {
            Resource resource = new ByteArrayResource(new byte[0]) {
                @Override
                public String getFilename() {
                    return "x.png";
                }
            };
            when(postService.getPostImage(1L)).thenReturn(resource);

            mockMvc.perform(get("/api/posts/1/image"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
        }

        @Test
        @DisplayName("returns 404 when post has no image")
        void returns404WhenNoImage() throws Exception {
            when(postService.getPostImage(1L)).thenReturn(null);

            mockMvc.perform(get("/api/posts/1/image"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 when post not found")
        void returns404WhenPostNotFound() throws Exception {
            when(postService.getPostImage(999L)).thenThrow(new NotFoundException("Post not found with id 999"));

            mockMvc.perform(get("/api/posts/999/image"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/posts (create JSON)")
    class CreatePostJson {

        @Test
        @DisplayName("returns 201 and post when authenticated and valid body")
        void returns201WhenValid() throws Exception {
            when(postService.createPost(anyLong(), any(PostCreateRequest.class))).thenReturn(SAMPLE_POST);

            mockMvc.perform(post("/api/posts")
                            .with(withPrincipal())
                            .contentType(APPLICATION_JSON)
                            .content("{\"title\":\"My Title\",\"content\":\"Content here\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Post created"))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("returns 400 when title blank")
        void returns400WhenTitleBlank() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .with(withPrincipal())
                            .contentType(APPLICATION_JSON)
                            .content("{\"title\":\"\",\"content\":\"Content\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("returns 400 when content blank")
        void returns400WhenContentBlank() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .with(withPrincipal())
                            .contentType(APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"content\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    @DisplayName("POST /api/posts (create multipart with image)")
    class CreatePostMultipart {

        @Test
        @Disabled("Multipart + @AuthenticationPrincipal in standalone MockMvc: principal can be null; use @SpringBootTest for full coverage")
        @DisplayName("returns 201 when authenticated with data and optional image")
        void returns201WithDataAndImage() throws Exception {
            when(postService.createPost(anyLong(), any(PostCreateRequest.class), any()))
                    .thenReturn(SAMPLE_POST_WITH_IMAGE);

            MockMultipartFile dataPart = new MockMultipartFile("data", "",
                    "application/json", "{\"title\":\"My Title\",\"content\":\"Content\"}".getBytes());
            MockMultipartFile imagePart = new MockMultipartFile("image", "pic.jpg",
                    "image/jpeg", new byte[] { 1, 2, 3 });

            mockMvc.perform(multipart("/api/posts")
                            .file(dataPart)
                            .file(imagePart)
                            .with(withPrincipal()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.imageUrl").value("/api/posts/1/image"));
        }

        @Test
        @Disabled("Multipart + @AuthenticationPrincipal in standalone MockMvc: principal can be null; use @SpringBootTest for full coverage")
        @DisplayName("returns 201 when only data part no image")
        void returns201WithDataOnly() throws Exception {
            when(postService.createPost(anyLong(), any(PostCreateRequest.class), eq(null)))
                    .thenReturn(SAMPLE_POST);

            MockMultipartFile dataPart = new MockMultipartFile("data", "",
                    "application/json", "{\"title\":\"My Title\",\"content\":\"Content\"}".getBytes());

            mockMvc.perform(multipart("/api/posts")
                            .file(dataPart)
                            .with(withPrincipal()))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/posts/{id} (update JSON)")
    class UpdatePostJson {

        @Test
        @DisplayName("returns 200 when author updates")
        void returns200WhenAuthorUpdates() throws Exception {
            PostResponse updated = new PostResponse(1L, "New Title", "new-title", "New content",
                    10L, "alice", null, NOW, NOW);
            when(postService.updatePost(eq(1L), anyLong(), any(PostUpdateRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/api/posts/1")
                            .with(withPrincipal())
                            .contentType(APPLICATION_JSON)
                            .content("{\"title\":\"New Title\",\"content\":\"New content\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("New Title"));
        }

        @Test
        @DisplayName("returns 404 when non-author updates")
        void returns404WhenNonAuthorUpdates() throws Exception {
            when(postService.updatePost(eq(1L), anyLong(), any(PostUpdateRequest.class)))
                    .thenThrow(new NotFoundException("Post not found"));

            mockMvc.perform(put("/api/posts/1")
                            .with(withPrincipal())
                            .contentType(APPLICATION_JSON)
                            .content("{\"title\":\"Hacked\",\"content\":\"x\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Post not found"));
        }

    }

    @Nested
    @DisplayName("PUT /api/posts/{id} (update multipart with image)")
    class UpdatePostMultipart {

        @Test
        @DisplayName("returns 200 when author updates with new image")
        void returns200WhenAuthorUpdatesWithImage() throws Exception {
            when(postService.updatePost(eq(1L), anyLong(), any(PostUpdateRequest.class), any()))
                    .thenReturn(SAMPLE_POST_WITH_IMAGE);

            MockMultipartFile dataPart = new MockMultipartFile("data", "",
                    "application/json", "{\"title\":\"Title\",\"content\":\"Content\"}".getBytes());
            MockMultipartFile imagePart = new MockMultipartFile("image", "new.png",
                    "image/png", new byte[] { 1, 2, 3 });

            mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/posts/1")
                            .file(dataPart)
                            .file(imagePart)
                            .with(withPrincipal()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Post updated"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/posts/{id}")
    class DeletePost {

        @Test
        @DisplayName("returns 200 when author deletes")
        void returns200WhenAuthorDeletes() throws Exception {
            mockMvc.perform(delete("/api/posts/1").with(withPrincipal()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Post deleted"));

            verify(postService).deletePost(1L, PRINCIPAL_ID);
        }

        @Test
        @DisplayName("returns 404 when non-author deletes")
        void returns404WhenNonAuthorDeletes() throws Exception {
            doThrow(new NotFoundException("Post not found"))
                    .when(postService).deletePost(eq(1L), anyLong());

            mockMvc.perform(delete("/api/posts/1").with(withPrincipal()))
                    .andExpect(status().isNotFound());
        }

    }
}
