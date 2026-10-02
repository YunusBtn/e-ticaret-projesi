package com.yunus.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    NOT_FOUND("Kayıt bulunamadı", HttpStatus.NOT_FOUND),
    DUPLICATE_ENTRY("Kayıt zaten mevcut", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK("Yetersiz stok", HttpStatus.CONFLICT),
    INVALID_PAYMENT("Geçersiz ödeme", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("Geçersiz veri", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("Beklenmeyen bir hata oluştu", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CREDENTIALS("Kullanıcı adı veya şifre hatalı", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("Bu işlem için yetkiniz yok", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus status;

    ErrorType(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

}
