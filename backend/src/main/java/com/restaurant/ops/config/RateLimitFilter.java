package com.restaurant.ops.config;

import com.restaurant.ops.security.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private static final Set<String> LIMITED_PATHS = Set.of(
      "/api/auth/login",
      "/api/auth/forgot-password",
      "/api/auth/reset-password",
      "/api/public/invitations/activate",
      "/api/me/email-change/verify",
      "/api/me/phone-change/verify"
  );

  private final RateLimiter rateLimiter;
  private final AppProperties properties;

  public RateLimitFilter(RateLimiter rateLimiter, AppProperties properties) {
    this.rateLimiter = rateLimiter;
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if ("POST".equals(request.getMethod()) && LIMITED_PATHS.contains(request.getRequestURI())) {
      String key = request.getRequestURI() + ":" + request.getRemoteAddr();
      if (!rateLimiter.allow(key, properties.security().rateLimitPerMinute())) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"RATE_LIMITED\"}");
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
