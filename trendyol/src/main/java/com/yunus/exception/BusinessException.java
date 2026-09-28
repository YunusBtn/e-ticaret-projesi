package com.yunus.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorType errorType;

    public BusinessException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public BusinessException(ErrorType errorType, String detail) {
        super(errorType.getMessage() + " " + detail);
        this.errorType = errorType;

    }


}
