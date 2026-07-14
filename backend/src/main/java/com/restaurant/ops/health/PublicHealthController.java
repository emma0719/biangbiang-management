package com.restaurant.ops.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/health")
public class PublicHealthController {
  @GetMapping
  Map<String, String> health() {
    return Map.of("status", "ok");
  }
}
