package com.blog.xblog.blog.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blog.xblog.blog.entity.BlogEntity;

public interface BlogRepository extends JpaRepository<BlogEntity, Long> {

    Optional<BlogEntity> findBySlug(String slug);

    Optional<BlogEntity> findBySlugAndIdNot(String slug, Long id);
}

