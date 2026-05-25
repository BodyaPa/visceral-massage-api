package com.example.visceralmassageapi.media.exception;

import com.example.visceralmassageapi.common.exception.NotFoundException;

import java.util.UUID;

public class MediaAssetNotFoundException extends NotFoundException {

    public MediaAssetNotFoundException(UUID id) {
        super("Media asset not found: " + id);
    }
}
