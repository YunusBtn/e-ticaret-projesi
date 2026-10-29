package com.yunus.repository;

import com.yunus.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.usedAt = :now, t.updatedAt = :now "
            + "where t.id = :id and t.usedAt is null and t.revokedAt is null")
    int markUsed(@Param("id") Long id, @Param("now")LocalDateTime now);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now, t.updatedAt = :now "
            + "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("now")LocalDateTime now);



    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now, t.updatedAt = :now "
            + "where t.user.id = :userId and t.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now")LocalDateTime now);




}
