package com.restaurant.ops.invitation;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.security.AuthorizationService;
import com.restaurant.ops.security.SecureTokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {
  private final InvitationRepository invitations;
  private final SecureTokenService tokens;
  private final AuthorizationService authorization;
  private final AppProperties properties;

  public InvitationService(InvitationRepository invitations, SecureTokenService tokens, AuthorizationService authorization, AppProperties properties) {
    this.invitations = invitations;
    this.tokens = tokens;
    this.authorization = authorization;
    this.properties = properties;
  }

  @Transactional
  public CreatedInvitation create(Employee actor, Set<Position> positions) {
    requireManager(actor);
    String rawToken = tokens.newToken();
    Invitation invitation = new Invitation();
    invitation.setCreator(actor);
    invitation.setPositions(positions);
    invitation.setTokenHash(tokens.hash(rawToken));
    invitation.setExpiresAt(Instant.now().plus(properties.security().invitationHours(), ChronoUnit.HOURS));
    invitations.save(invitation);
    return new CreatedInvitation(invitation, activationLink(rawToken));
  }

  public List<Invitation> list(Employee actor) {
    requireManager(actor);
    return invitations.findAll();
  }

  public Invitation get(Employee actor, Long id) {
    requireManager(actor);
    return invitations.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND"));
  }

  @Transactional
  public CreatedInvitation regenerate(Employee actor, Long id) {
    Invitation invitation = get(actor, id);
    if (invitation.getStatus() == InvitationStatus.USED || invitation.getStatus() == InvitationStatus.REVOKED) {
      throw new ApiException(HttpStatus.CONFLICT, "INVITATION_NOT_REGENERATABLE");
    }
    String rawToken = tokens.newToken();
    invitation.setTokenHash(tokens.hash(rawToken));
    invitation.incrementTokenVersion();
    invitation.setStatus(InvitationStatus.ACTIVE);
    invitation.setExpiresAt(Instant.now().plus(properties.security().invitationHours(), ChronoUnit.HOURS));
    return new CreatedInvitation(invitation, activationLink(rawToken));
  }

  @Transactional
  public Invitation revoke(Employee actor, Long id) {
    Invitation invitation = get(actor, id);
    if (invitation.getStatus() == InvitationStatus.USED) {
      throw new ApiException(HttpStatus.CONFLICT, "INVITATION_ALREADY_USED");
    }
    invitation.revoke();
    return invitation;
  }

  public InvitationDtos.ValidateInvitationResponse validate(String rawToken) {
    return invitations.findByTokenHash(tokens.hash(rawToken))
        .filter(this::isActiveNow)
        .map(invitation -> new InvitationDtos.ValidateInvitationResponse(true, invitation.getPositions(), invitation.getExpiresAt(), "OK"))
        .orElseGet(() -> new InvitationDtos.ValidateInvitationResponse(false, Set.of(), null, "INVITATION_INVALID_OR_EXPIRED"));
  }

  @Transactional
  public Invitation lockValidInvitation(String rawToken) {
    Invitation invitation = invitations.findWithLockByTokenHash(tokens.hash(rawToken))
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVITATION_INVALID_OR_EXPIRED"));
    if (!isActiveNow(invitation)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVITATION_INVALID_OR_EXPIRED");
    }
    return invitation;
  }

  public InvitationDtos.InvitationResponse response(Invitation invitation, String activationLink) {
    return new InvitationDtos.InvitationResponse(
        invitation.getId(),
        invitation.getPositions(),
        invitation.getCreator().getId(),
        invitation.getCreatedAt(),
        invitation.getExpiresAt(),
        invitation.getUsedAt(),
        invitation.getRevokedAt(),
        invitation.getTokenVersion(),
        invitation.getStatus(),
        activationLink
    );
  }

  private boolean isActiveNow(Invitation invitation) {
    return invitation.getStatus() == InvitationStatus.ACTIVE && invitation.getExpiresAt().isAfter(Instant.now());
  }

  private String activationLink(String rawToken) {
    return properties.publicBaseUrl() + "/activate?token=" + rawToken;
  }

  private void requireManager(Employee actor) {
    if (!authorization.isManager(actor)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_MANAGER_REQUIRED");
    }
  }

  public record CreatedInvitation(Invitation invitation, String activationLink) {}
}
