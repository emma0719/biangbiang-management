package com.restaurant.ops.auth;

import com.restaurant.ops.common.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/verification")
public class DevelopmentVerificationController {
  private final DevelopmentOutboxNotifier notifier;

  public DevelopmentVerificationController(DevelopmentOutboxNotifier notifier) {
    this.notifier = notifier;
  }

  @GetMapping("/latest")
  DevelopmentOutboxNotifier.VerificationMessage latest(@RequestParam String purpose, @RequestParam String channel, @RequestParam String destination) {
    return notifier.latest(purpose, channel, destination)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEV_VERIFICATION_NOT_FOUND"));
  }
}
