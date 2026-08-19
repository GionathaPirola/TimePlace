package com.timeplace.backend.exception;

public class PhotoNotFoundException extends RuntimeException {

    public PhotoNotFoundException(long photoId) {
        super("Photo not found: " + photoId);
    }
}
