package edusecure.edusecure.repository;

import edusecure.edusecure.entity.MfaRecoveryCode;
import edusecure.edusecure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

    List<MfaRecoveryCode> findAllByUserAndUsedAtIsNull(User user);

    long countByUserAndUsedAtIsNull(User user);

    long deleteByUser(User user);
}

