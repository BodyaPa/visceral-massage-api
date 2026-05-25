package com.example.visceralmassageapi.media.storage;

import com.example.visceralmassageapi.common.config.MediaProps;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalMediaFileStorage implements MediaFileStorage {

    private final Path storageDirectory;

    public LocalMediaFileStorage(MediaProps properties) {
        this.storageDirectory = properties.getStorageDirectory().toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream content) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(storageDirectory);
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store media file", ex);
        }
    }

    @Override
    public Resource load(String storageKey) {
        Resource resource = new FileSystemResource(resolve(storageKey));
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Stored media file is unavailable");
        }
        return resource;
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete media file", ex);
        }
    }

    private Path resolve(String storageKey) {
        Path target = storageDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid media storage key");
        }
        return target;
    }
}
