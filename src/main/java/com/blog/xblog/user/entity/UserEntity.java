




package com.blog.xblog.user.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.blog.xblog.blog.entity.BlogEntity;

@Entity
@Table(name = "user_entity", indexes = {
  @Index(name = "idx_username", columnList = "username"),
  @Index(name = "idx_email", columnList = "email")
})
@NoArgsConstructor
@Builder
@ToString(exclude = "blogs")
@EqualsAndHashCode
@Getter
@Setter
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", unique = true , nullable = false)
  private String username;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "email", unique = true, nullable = false)
  private String email;

  @OneToMany(mappedBy = "author")
  private List<BlogEntity> blogs = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

    public UserEntity(Long id,
                      String username,
                      String password,
                      String email,
                      List<BlogEntity> blogs,
                      Instant createdAt,
                      Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.blogs = blogs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UserEntity(String username, String password, String email) {
        this(null, username, password, email, new ArrayList<>(), Instant.now(), Instant.now());
    }
}