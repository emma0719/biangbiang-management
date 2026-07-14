package com.restaurant.ops.invitation;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
  Optional<Invitation> findByTokenHash(String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Invitation i where i.tokenHash = :tokenHash")
  Optional<Invitation> findWithLockByTokenHash(String tokenHash);
}
