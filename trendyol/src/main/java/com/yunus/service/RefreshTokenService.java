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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
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

        if (refreshTokenRepository.markUsed(token.getId(), now) == 0) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            log.warn("Refresh token yeniden kullanıldı, aile iptal edildi. user: {}", token.getUser().getId());
            throw invalidToken();
        }

        String newRaw = issue(token.getUser(), token.getFamilyId());
        return new RotatedToken(token.getUser(), newRaw);



    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorType.AUTHENTICATION_REQUIRED);
    }




    private String issue(User user, String familyId) {
        String raw = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .familyId(familyId)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .build();

        refreshTokenRepository.save(entity);

        return raw;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 desteklenmiyor", e);
        }
    }





}
