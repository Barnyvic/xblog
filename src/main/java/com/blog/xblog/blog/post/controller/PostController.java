package com.blog.xblog.blog.post.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blog.xblog.blog.post.dto.PostCreateRequest;
import com.blog.xblog.blog.post.dto.PostResponse;
import com.blog.xblog.blog.post.dto.PostUpdateRequest;
import com.blog.xblog.blog.post.service.PostService;
import com.blog.xblog.common.dto.ApiResponse;
import com.blog.xblog.common.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "Blog post CRUD (create/update/delete require JWT)")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "Create post (JSON)", description = "Create a new post as the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostCreateRequest request) {
        PostResponse post = postService.createPost(principal.getId(), request);
        ApiResponse<PostResponse> body = ApiResponse.<PostResponse>builder()
                .success(true)
                .message("Post created")
                .data(post)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Create post (multipart with optional image)", description = "Create a new post with optional featured image")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPostWithImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestPart("data") PostCreateRequest request,
            @Parameter(description = "Optional featured image (JPEG/PNG, max 5MB)") @RequestPart(value = "image", required = false) MultipartFile image) {
        PostResponse post = postService.createPost(principal.getId(), request, image);
        ApiResponse<PostResponse> body = ApiResponse.<PostResponse>builder()
                .success(true)
                .message("Post created")
                .data(post)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Get post by ID", description = "Returns a single post (public)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @Parameter(description = "Post ID") @PathVariable Long id) {
        PostResponse post = postService.getPost(id);
        ApiResponse<PostResponse> body = ApiResponse.<PostResponse>builder()
                .success(true)
                .message("Post details")
                .data(post)
                .build();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Get post image", description = "Returns the post's featured image (public). 404 if no image.")
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getPostImage(
            @Parameter(description = "Post ID") @PathVariable Long id) throws IOException {
        Resource resource = postService.getPostImage(id);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String filename = resource.getFilename();
        MediaType mediaType = filename != null && filename.toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }

    @Operation(summary = "List posts", description = "Returns all posts (public)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponse>>> listPosts() {
        List<PostResponse> posts = postService.listPosts();
        ApiResponse<List<PostResponse>> body = ApiResponse.<List<PostResponse>>builder()
                .success(true)
                .message("Posts list")
                .data(posts)
                .build();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Update post (JSON)", description = "Update a post (author only)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @Parameter(description = "Post ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PostUpdateRequest request) {
        PostResponse post = postService.updatePost(id, principal.getId(), request);
        ApiResponse<PostResponse> body = ApiResponse.<PostResponse>builder()
                .success(true)
                .message("Post updated")
                .data(post)
                .build();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Update post (multipart with optional image)", description = "Update a post and optionally replace featured image (author only)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> updatePostWithImage(
            @Parameter(description = "Post ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestPart("data") PostUpdateRequest request,
            @Parameter(description = "Optional new featured image (JPEG/PNG, max 5MB)") @RequestPart(value = "image", required = false) MultipartFile image) {
        PostResponse post = postService.updatePost(id, principal.getId(), request, image);
        ApiResponse<PostResponse> body = ApiResponse.<PostResponse>builder()
                .success(true)
                .message("Post updated")
                .data(post)
                .build();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Delete post", description = "Delete a post (author only)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "Post ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal) {
        postService.deletePost(id, principal.getId());
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(true)
                .message("Post deleted")
                .build();
        return ResponseEntity.ok(body);
    }
}
