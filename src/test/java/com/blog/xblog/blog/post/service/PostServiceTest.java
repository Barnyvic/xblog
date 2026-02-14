package com.blog.xblog.blog.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.blog.xblog.blog.entity.BlogEntity;
import com.blog.xblog.blog.post.dto.PostCreateRequest;
import com.blog.xblog.blog.post.dto.PostResponse;
import com.blog.xblog.blog.post.dto.PostUpdateRequest;
import com.blog.xblog.blog.post.repository.BlogRepository;
import com.blog.xblog.common.exception.NotFoundException;
import com.blog.xblog.common.storage.FileStorageService;
import com.blog.xblog.user.entity.UserEntity;
import com.blog.xblog.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

    private PostService postService;

    private static final Instant NOW = Instant.parse("2026-02-14T12:00:00Z");
    private static final UserEntity AUTHOR = userEntity(10L, "alice");

    @BeforeEach
    void setUp() {
        postService = new PostService(blogRepository, userService, fileStorageService);
    }

    @Nested
    @DisplayName("createPost (JSON, no image)")
    class CreatePost {

        @Test
        @DisplayName("saves entity and returns response when author exists")
        void savesAndReturnsWhenAuthorExists() {
            PostCreateRequest request = new PostCreateRequest("My Title", "Content");
            when(userService.findById(10L)).thenReturn(Optional.of(AUTHOR));
            when(blogRepository.findBySlug("my-title")).thenReturn(Optional.empty());
            BlogEntity saved = blogEntity(1L, "My Title", "my-title", "Content", AUTHOR, null);
            when(blogRepository.save(any(BlogEntity.class))).thenReturn(saved);

            PostResponse result = postService.createPost(10L, request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("My Title");
            assertThat(result.slug()).isEqualTo("my-title");
            assertThat(result.authorId()).isEqualTo(10L);
            assertThat(result.authorUsername()).isEqualTo("alice");
            assertThat(result.imageUrl()).isNull();
            verify(fileStorageService, never()).savePostImage(anyLong(), any());
        }

        @Test
        @DisplayName("throws NotFoundException when author not found")
        void throwsWhenAuthorNotFound() {
            when(userService.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.createPost(999L, new PostCreateRequest("T", "C")))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Author not found with id 999");
            verify(blogRepository, never()).save(any());
        }

        @Test
        @DisplayName("uses unique slug when base slug taken")
        void usesUniqueSlugWhenTaken() {
            PostCreateRequest request = new PostCreateRequest("Hello", "Content");
            when(userService.findById(10L)).thenReturn(Optional.of(AUTHOR));
            when(blogRepository.findBySlug("hello")).thenReturn(Optional.of(blogEntity(1L, "x", "hello", "x", AUTHOR, null)));
            when(blogRepository.findBySlug("hello-1")).thenReturn(Optional.empty());
            BlogEntity saved = blogEntity(2L, "Hello", "hello-1", "Content", AUTHOR, null);
            when(blogRepository.save(any(BlogEntity.class))).thenReturn(saved);

            PostResponse result = postService.createPost(10L, request);

            assertThat(result.slug()).isEqualTo("hello-1");
        }
    }

    @Nested
    @DisplayName("createPost with image")
    class CreatePostWithImage {

        @Test
        @DisplayName("saves image and sets imagePath when image provided")
        void savesImageAndSetsPath() {
            PostCreateRequest request = new PostCreateRequest("Title", "Content");
            MultipartFile image = mockMultipartFile("image/jpeg", "x.jpg");
            when(userService.findById(10L)).thenReturn(Optional.of(AUTHOR));
            when(blogRepository.findBySlug("title")).thenReturn(Optional.empty());
            BlogEntity firstSave = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            BlogEntity withImage = blogEntity(1L, "Title", "title", "Content", AUTHOR, "posts/1/abc.jpg");
            when(blogRepository.save(any(BlogEntity.class))).thenReturn(firstSave).thenReturn(withImage);
            when(fileStorageService.savePostImage(1L, image)).thenReturn("posts/1/abc.jpg");

            PostResponse result = postService.createPost(10L, request, image);

            assertThat(result.imageUrl()).isEqualTo("/api/posts/1/image");
            verify(fileStorageService).savePostImage(1L, image);
        }

        @Test
        @DisplayName("ignores null image")
        void ignoresNullImage() {
            PostCreateRequest request = new PostCreateRequest("Title", "Content");
            when(userService.findById(10L)).thenReturn(Optional.of(AUTHOR));
            when(blogRepository.findBySlug("title")).thenReturn(Optional.empty());
            BlogEntity saved = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            when(blogRepository.save(any(BlogEntity.class))).thenReturn(saved);

            postService.createPost(10L, request, null);

            verify(fileStorageService, never()).savePostImage(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("returns post when found")
        void returnsPostWhenFound() {
            BlogEntity entity = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(entity));

            PostResponse result = postService.getPost(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Title");
        }

        @Test
        @DisplayName("throws NotFoundException when not found")
        void throwsWhenNotFound() {
            when(blogRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPost(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Post not found with id 999");
        }
    }

    @Nested
    @DisplayName("listPosts")
    class ListPosts {

        @Test
        @DisplayName("returns all posts from repository")
        void returnsAll() {
            BlogEntity a = blogEntity(1L, "A", "a", "C", AUTHOR, null);
            BlogEntity b = blogEntity(2L, "B", "b", "C", AUTHOR, null);
            when(blogRepository.findAll()).thenReturn(List.of(a, b));

            List<PostResponse> result = postService.listPosts();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).title()).isEqualTo("A");
            assertThat(result.get(1).title()).isEqualTo("B");
        }

        @Test
        @DisplayName("returns empty list when no posts")
        void returnsEmptyWhenNone() {
            when(blogRepository.findAll()).thenReturn(List.of());

            List<PostResponse> result = postService.listPosts();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePost (author check)")
    class UpdatePost {

        @Test
        @DisplayName("updates and returns when caller is author")
        void updatesWhenAuthor() {
            BlogEntity post = blogEntity(1L, "Old", "old", "Content", AUTHOR, null);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));
            when(blogRepository.findBySlugAndIdNot("new-title", 1L)).thenReturn(Optional.empty());
            when(blogRepository.save(any(BlogEntity.class))).thenAnswer(i -> i.getArgument(0));

            PostUpdateRequest request = new PostUpdateRequest("New Title", "New content");
            PostResponse result = postService.updatePost(1L, 10L, request);

            assertThat(result.title()).isEqualTo("New Title");
            assertThat(result.slug()).isEqualTo("new-title");
            verify(blogRepository).save(post);
        }

        @Test
        @DisplayName("throws NotFoundException when caller is not author")
        void throwsWhenNotAuthor() {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(1L, 20L, new PostUpdateRequest("X", "Y")))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Post not found");
            verify(blogRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws NotFoundException when post does not exist")
        void throwsWhenPostNotFound() {
            when(blogRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(999L, 10L, new PostUpdateRequest("X", "Y")))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Post not found with id 999");
        }
    }

    @Nested
    @DisplayName("updatePost with image")
    class UpdatePostWithImage {

        @Test
        @DisplayName("deletes old image and saves new when image provided")
        void replacesImageWhenProvided() {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, "posts/1/old.jpg");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));
            when(blogRepository.save(any(BlogEntity.class))).thenAnswer(i -> i.getArgument(0));
            MultipartFile image = mockMultipartFile("image/png", "new.png");
            when(fileStorageService.savePostImage(1L, image)).thenReturn("posts/1/new.png");

            postService.updatePost(1L, 10L, new PostUpdateRequest(null, null), image);

            verify(fileStorageService).deleteByRelativePath("posts/1/old.jpg");
            verify(fileStorageService).savePostImage(1L, image);
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("deletes post and image file when author")
        void deletesWhenAuthor() {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, "posts/1/x.jpg");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));

            postService.deletePost(1L, 10L);

            verify(fileStorageService).deleteByRelativePath("posts/1/x.jpg");
            verify(blogRepository).delete(post);
        }

        @Test
        @DisplayName("does not call fileStorage when post has no image")
        void noFileDeleteWhenNoImage() {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));

            postService.deletePost(1L, 10L);

            verify(fileStorageService).deleteByRelativePath(null);
            verify(blogRepository).delete(post);
        }

        @Test
        @DisplayName("throws NotFoundException when caller is not author")
        void throwsWhenNotAuthor() {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.deletePost(1L, 20L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Post not found");
            verify(blogRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getPostImage")
    class GetPostImage {

        @Test
        @DisplayName("returns resource when post has imagePath")
        void returnsResourceWhenHasImage() throws Exception {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, "posts/1/x.jpg");
            Resource resource = org.mockito.Mockito.mock(Resource.class);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));
            when(fileStorageService.getResource("posts/1/x.jpg")).thenReturn(resource);

            Resource result = postService.getPostImage(1L);

            assertThat(result).isSameAs(resource);
        }

        @Test
        @DisplayName("returns null when post has no image")
        void returnsNullWhenNoImage() throws Exception {
            BlogEntity post = blogEntity(1L, "Title", "title", "Content", AUTHOR, null);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(post));

            Resource result = postService.getPostImage(1L);

            assertThat(result).isNull();
            verify(fileStorageService, never()).getResource(any());
        }

        @Test
        @DisplayName("throws NotFoundException when post not found")
        void throwsWhenPostNotFound() {
            when(blogRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPostImage(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Post not found with id 999");
        }
    }

    private static UserEntity userEntity(Long id, String username) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .email(username + "@example.com")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static BlogEntity blogEntity(Long id, String title, String slug, String content,
                                        UserEntity author, String imagePath) {
        return BlogEntity.builder()
                .id(id)
                .title(title)
                .slug(slug)
                .content(content)
                .author(author)
                .createdAt(NOW)
                .updatedAt(NOW)
                .imagePath(imagePath)
                .build();
    }

    private static MultipartFile mockMultipartFile(String contentType, String name) {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        org.mockito.Mockito.lenient().when(file.isEmpty()).thenReturn(false);
        org.mockito.Mockito.lenient().when(file.getContentType()).thenReturn(contentType);
        org.mockito.Mockito.lenient().when(file.getOriginalFilename()).thenReturn(name);
        org.mockito.Mockito.lenient().when(file.getSize()).thenReturn(100L);
        return file;
    }
}
