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
    ACCESS_DENIED("Bu işlem için yetkiniz yok", HttpStatus.FORBIDDEN),
    LAST_ADMIN_PROTECTED("Sistemdeki son yönetici silinemez veya yetkisi düşürülemez", HttpStatus.CONFLICT),
    CONCURRENT_UPDATE("Kayıt başka bir işlem tarafından değiştirildi, lütfen tekrar deneyin", HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION("Sipariş durumu bu şekilde değiştirilemez", HttpStatus.CONFLICT),
    USER_HAS_ACTIVE_ORDERS("Kullanıcı aktif siparişlerine sahip olduğu için silinemez", HttpStatus.CONFLICT),
    AUTHENTICATION_REQUIRED("Kimlik doğrulaması gerekli veya oturum süresi doldu", HttpStatus.UNAUTHORIZED),
    MALFORMED_REQUEST("İstek okunamadı, lütfen geçerli bir istek gönderin", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("Bu adres için bu HTTP metodu desteklenmiyor", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("Desteklenmeyen içerik türü, JSON gönderin", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS("Çok fazla istek, lütfen daha sonra tekrar deneyin", HttpStatus.TOO_MANY_REQUESTS);

    private final String message;
    private final HttpStatus status;

    ErrorType(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}