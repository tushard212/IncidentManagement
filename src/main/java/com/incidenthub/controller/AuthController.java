package com.incidenthub.controller;

import com.incidenthub.dto.AuthDto;
import com.incidenthub.ratelimiter.RateLimit;
import com.incidenthub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @RateLimit(maxRequests = 5, windowSeconds = 300, keyType = RateLimit.KeyType.IP)
  public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/login")
  @RateLimit(maxRequests = 10, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
  public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
