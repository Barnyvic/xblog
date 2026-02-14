package com.blog.xblog.blog.post.service;

import java.io.IOException;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.blog.xblog.blog.entity.BlogEntity;
import com.blog.xblog.blog.post.dto.PostCreateRequest;
import com.blog.xblog.blog.post.dto.PostResponse;
import com.blog.xblog.blog.post.dto.PostUpdateRequest;
import com.blog.xblog.blog.post.repository.BlogRepository;
import com.blog.xblog.common.exception.NotFoundException;
import com.blog.xblog.common.mapper.PostMapper;
import com.blog.xblog.common.storage.FileStorageService;
import com.blog.xblog.common.util.DateTimeUtil;
import com.blog.xblog.common.util.SlugUtil;
import com.blog.xblog.user.entity.UserEntity;
import com.blog.xblog.user.service.UserService;

@Service
public class PostService {

    private final BlogRepository blogRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public PostService(BlogRepository blogRepository, UserService userService, FileStorageService fileStorageService) {
        this.blogRepository = blogRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public PostResponse createPost(Long authorId, PostCreateRequest request) {
        return createPost(authorId, request, null);
    }

    @Transactional
    public PostResponse createPost(Long authorId, PostCreateRequest request, MultipartFile image) {
        UserEntity author = userService.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found with id " + authorId));

        String baseSlug = SlugUtil.toSlug(request.title());
        String uniqueSlug = ensureUniqueSlug(baseSlug);

        BlogEntity entity = PostMapper.toBlogEntity(
                request,
                author,
                uniqueSlug,
                DateTimeUtil.now(),
                DateTimeUtil.now()
        );

        entity = blogRepository.save(entity);

        if (image != null && !image.isEmpty()) {
            String relativePath = fileStorageService.savePostImage(entity.getId(), image);
            entity.setImagePath(relativePath);
            entity = blogRepository.save(entity);
        }

        return PostMapper.toPostResponse(entity);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "posts", key = "#id")
    public PostResponse getPost(Long id) {
        return PostMapper.toPostResponse(findPostOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> listPosts() {
        return PostMapper.toPostResponses(blogRepository.findAll());
    }

    @Transactional
    @CacheEvict(cacheNames = "posts", key = "#id")
    public PostResponse updatePost(Long id, Long authorId, PostUpdateRequest request) {
        return updatePost(id, authorId, request, null);
    }

    @Transactional
    @CacheEvict(cacheNames = "posts", key = "#id")
    public PostResponse updatePost(Long id, Long authorId, PostUpdateRequest request, MultipartFile image) {
        BlogEntity post = findPostOrThrow(id);

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new NotFoundException("Post not found");
        }

        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteByRelativePath(post.getImagePath());
            String relativePath = fileStorageService.savePostImage(id, image);
            post.setImagePath(relativePath);
        }

        PostMapper.applyUpdate(request, post, DateTimeUtil.now());

        if (request.title() != null && !request.title().isBlank()) {
            post.setSlug(ensureUniqueSlugExcluding(SlugUtil.toSlug(post.getTitle()), id));
        }

        post = blogRepository.save(post);
        return PostMapper.toPostResponse(post);
    }

    @Transactional
    @CacheEvict(cacheNames = "posts", key = "#id")
    public void deletePost(Long id, Long authorId) {
        BlogEntity post = findPostOrThrow(id);

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new NotFoundException("Post not found");
        }

        fileStorageService.deleteByRelativePath(post.getImagePath());
        blogRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Resource getPostImage(Long id) throws IOException {
        BlogEntity post = findPostOrThrow(id);
        String imagePath = post.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        return fileStorageService.getResource(imagePath);
    }

    private BlogEntity findPostOrThrow(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found with id " + id));
    }

    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        while (blogRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    private String ensureUniqueSlugExcluding(String baseSlug, Long excludeId) {
        String slug = baseSlug;
        int counter = 1;
        while (blogRepository.findBySlugAndIdNot(slug, excludeId).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}

