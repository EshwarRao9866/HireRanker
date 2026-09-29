package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.eshwar.entity.PasswordResetToken;
import spring.eshwar.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    Optional<PasswordResetToken> findTopByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllActiveTokensForUser(@Param("user") User user);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);
}
