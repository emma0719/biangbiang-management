package com.restaurant.ops.invitation;

import com.restaurant.ops.auth.AuthDtos;
import com.restaurant.ops.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/invitations")
public class PublicInvitationController {
  private final InvitationService invitationService;
  private final AuthService authService;

  public PublicInvitationController(InvitationService invitationService, AuthService authService) {
    this.invitationService = invitationService;
    this.authService = authService;
  }

  @GetMapping("/validate")
  InvitationDtos.ValidateInvitationResponse validate(@RequestParam String token) {
    return invitationService.validate(token);
  }

  @PostMapping("/activate")
  AuthDtos.ActivationResponse activate(@Valid @RequestBody AuthDtos.ActivationRequest request) {
    return authService.activate(request);
  }
}
