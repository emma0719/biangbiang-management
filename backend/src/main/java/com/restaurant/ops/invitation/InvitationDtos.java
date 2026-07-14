package com.restaurant.ops.invitation;

import com.restaurant.ops.employee.Position;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.Set;

public class InvitationDtos {
  public record CreateInvitationRequest(@NotEmpty Set<Position> positions) {}
  public record InvitationResponse(
      Long id,
      Set<Position> positions,
      Long creatorId,
      Instant createdAt,
      Instant expiresAt,
      Instant usedAt,
      Instant revokedAt,
      int tokenVersion,
      InvitationStatus status,
      String activationLink
  ) {}
  public record ValidateInvitationResponse(boolean valid, Set<Position> positions, Instant expiresAt, String code) {}
}
