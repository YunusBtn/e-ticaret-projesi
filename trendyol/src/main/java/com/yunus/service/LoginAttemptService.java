package com.yunus.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class LoginAttemptService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Cache<String, Integer> failedAttempts = Caffeine.newBuilder()
            .expireAfterWrite(LOCK_DURATION)
            .maximumSize(10_000)
            .build();

    public boolean isBlocked(String username) {
        Integer count = failedAttempts.getIfPresent(username);
        return count != null && count >= MAX_FAILED_ATTEMPTS;
    }


    //merge atomik : iki istek aynı anda başarısız olsa bile sayaç doğru atar.
    //değer son başarısız denemeden itibaren hesaplanır.
    public void recordFailure(String username) {
        int count = failedAttempts.asMap().merge(username, 1, Integer::sum);
        if (count > MAX_FAILED_ATTEMPTS) {
            log.warn("Giriş deneme sınırı aşıldı, kullanıcı adı geçici olarak kilitlendi : {}", username);

        }
    }

    public void recordSuccess(String username) {
        failedAttempts.invalidate(username);
    }


}
