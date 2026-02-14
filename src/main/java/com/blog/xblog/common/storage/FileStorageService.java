package com.blog.xblog.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.blog.xblog.common.exception.BadRequestException;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; 

    private final Path basePath;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String savePostImage(Long postId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG and PNG images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Image size must not exceed 5MB");
        }

        String extension = contentType.equals("image/png") ? "png" : "jpg";
        String safeFilename = UUID.randomUUID() + "." + extension;
        String relativePath = "posts/" + postId + "/" + safeFilename;
        Path targetPath = resolveAndValidateRelative(relativePath);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save image", e);
        }

        return relativePath;
    }

    public void deleteByRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path path = resolveAndValidateRelative(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
        }
    }

    public Resource getResource(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path path = resolveAndValidateRelative(relativePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return null;
        }
        return new UrlResource(path.toUri());
    }

    private Path resolveAndValidateRelative(String relativePath) {
        if (relativePath.contains("..")) {
            throw new BadRequestException("Invalid path");
        }
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new BadRequestException("Invalid path");
        }
        return resolved;
    }
}
