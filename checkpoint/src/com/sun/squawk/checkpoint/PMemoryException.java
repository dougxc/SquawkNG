package com.sun.squawk.checkpoint;

/**
 * Thrown by the persistent memory manager to indicate a error
 * interacting with the persistent object store.
 */
public class PMemoryException extends RuntimeException {

    public PMemoryException() {
    }

    public PMemoryException(String detailMessage) {
        super(detailMessage);
    }
}