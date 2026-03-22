package edusecure.edusecure.repository.auth;

import edusecure.edusecure.entity.auth.MfaChallenge;
import edusecure.edusecure.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

    long deleteByExpiresAtBefore(Instant instant);

    long deleteByUser(User user);
}