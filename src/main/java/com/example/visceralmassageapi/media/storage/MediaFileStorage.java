package com.example.visceralmassageapi.media.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface MediaFileStorage {

    void store(String storageKey, InputStream content);

    Resource load(String storageKey);

    void delete(String storageKey);
}
