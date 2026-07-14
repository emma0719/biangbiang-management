package com.restaurant.ops.invitation;

import com.restaurant.ops.auth.AppPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/invitations")
public class ManagerInvitationController {
  private final InvitationService invitationService;

  public ManagerInvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @PostMapping
  InvitationDtos.InvitationResponse create(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody InvitationDtos.CreateInvitationRequest request) {
    InvitationService.CreatedInvitation created = invitationService.create(principal.employee(), request.positions());
    return invitationService.response(created.invitation(), created.activationLink());
  }

  @GetMapping
  List<InvitationDtos.InvitationResponse> list(@AuthenticationPrincipal AppPrincipal principal) {
    return invitationService.list(principal.employee()).stream()
        .map(invitation -> invitationService.response(invitation, null))
        .toList();
  }

  @GetMapping("/{id}")
  InvitationDtos.InvitationResponse get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return invitationService.response(invitationService.get(principal.employee(), id), null);
  }

  @PostMapping("/{id}/regenerate")
  InvitationDtos.InvitationResponse regenerate(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    InvitationService.CreatedInvitation created = invitationService.regenerate(principal.employee(), id);
    return invitationService.response(created.invitation(), created.activationLink());
  }

  @PostMapping("/{id}/revoke")
  InvitationDtos.InvitationResponse revoke(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return invitationService.response(invitationService.revoke(principal.employee(), id), null);
  }
}
