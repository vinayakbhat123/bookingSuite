package com.vinayakbhat.bookingSuite.common.security.Exception;

public class UnAuthorisedException extends RuntimeException {
    public UnAuthorisedException(String message) {
        super(message);
    }
}
