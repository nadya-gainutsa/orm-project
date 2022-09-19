package org.example.exception;

public class ORMException extends RuntimeException {
    public ORMException(String message, Throwable e) {
        super(message, e);
    }
}
