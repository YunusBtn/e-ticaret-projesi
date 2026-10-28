package com.yunus.repository;

import com.yunus.entity.User;
import com.yunus.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = :role order by u.id")
    List<User> findAllByRoleForUpdate(@Param("role") Role role);

    boolean existsByRole(Role role);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    boolean existByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
}
