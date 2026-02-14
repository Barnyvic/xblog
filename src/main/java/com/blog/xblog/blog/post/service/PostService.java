package com.blog.xblog.blog.post.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blog.xblog.blog.entity.BlogEntity;
import com.blog.xblog.blog.post.dto.PostCreateRequest;
import com.blog.xblog.blog.post.dto.PostResponse;
import com.blog.xblog.blog.post.dto.PostUpdateRequest;
import com.blog.xblog.blog.post.repository.BlogRepository;
import com.blog.xblog.common.exception.NotFoundException;
import com.blog.xblog.common.mapper.PostMapper;
import com.blog.xblog.common.util.DateTimeUtil;
import com.blog.xblog.common.util.SlugUtil;
import com.blog.xblog.user.entity.UserEntity;
import com.blog.xblog.user.service.UserService;

@Service
public class PostService {

    private final BlogRepository blogRepository;
    private final UserService userService;

    public PostService(BlogRepository blogRepository, UserService userService) {
        this.blogRepository = blogRepository;
        this.userService = userService;
    }

    @Transactional
    public PostResponse createPost(Long authorId, PostCreateRequest request) {
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
        BlogEntity post = findPostOrThrow(id);

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new NotFoundException("Post not found"); 
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

        blogRepository.delete(post);
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

