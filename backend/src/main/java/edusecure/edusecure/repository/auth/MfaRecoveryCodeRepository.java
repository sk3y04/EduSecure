package edusecure.edusecure.repository.auth;

import edusecure.edusecure.entity.auth.MfaRecoveryCode;
import edusecure.edusecure.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

    List<MfaRecoveryCode> findAllByUserAndUsedAtIsNull(User user);

    long countByUserAndUsedAtIsNull(User user);

    long deleteByUser(User user);
}