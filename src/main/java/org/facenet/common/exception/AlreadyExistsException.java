package org.facenet.common.exception;

/**
 * Exception thrown when attempting to create a resource that already exists
 */
public class AlreadyExistsException extends RuntimeException {
    
    public AlreadyExistsException(String message) {
        super(message);
    }
    
    public AlreadyExistsException(String resource, String field, Object value) {
        super(String.format("%s with %s '%s' already exists", resource, field, value));
    }
}
