package com.cts.mfrp.oa.exception;

public class TopUpLimitExceededException extends RuntimeException {
    public TopUpLimitExceededException(String message) {
        super(message);
    }
}
