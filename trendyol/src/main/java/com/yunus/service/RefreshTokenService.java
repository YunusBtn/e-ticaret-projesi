package com.yunus.service;

import com.yunus.entity.RefreshToken;
import com.yunus.entity.User;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;



@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public record RotatedToken(User user, String refreshToken) {
    }

    @Transactional
    public String createForLogin(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RotatedToken rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(this::invalidToken);

        LocalDateTime now = LocalDateTime.now();

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(now) || token.getUser().getDeletedAt() != null) {
            throw invalidToken();
        }

        if (refreshTokenRepository.markUsed(token.getId(), now) == 0 ) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            log.warn("Refresh token yeniden kullanıldı, aile iptal edildi. user: {}", token.getUser().getId());
            throw invalidToken();
        }

        return new RotatedToken(token.getUser(), issue(token.getUser(), UUID.randomUUID().toString()));
    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorType.AUTHENTICATION_REQUIRED);
    }


}
