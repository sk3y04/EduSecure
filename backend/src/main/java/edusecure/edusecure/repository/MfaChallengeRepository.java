package edusecure.edusecure.repository;

import edusecure.edusecure.entity.MfaChallenge;
import edusecure.edusecure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

    long deleteByExpiresAtBefore(Instant instant);

    long deleteByUser(User user);
}

