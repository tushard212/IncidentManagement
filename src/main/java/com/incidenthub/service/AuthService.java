package com.incidenthub.service;

import com.incidenthub.dto.AuthDto;
import com.incidenthub.model.Team;
import com.incidenthub.model.User;
import com.incidenthub.model.enums.Role;
import com.incidenthub.repository.TeamRepository;
import com.incidenthub.repository.UserRepository;
import com.incidenthub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;

  @Transactional
  public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("Username already exists");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    User user = User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .fullName(request.getFullName())
        .phone(request.getPhone())
        .role(request.getRole() != null ? request.getRole() : Role.ENGINEER)
        .isOnCall(false)
        .build();

    if (request.getTeamId() != null) {
      Team team = teamRepository.findById(request.getTeamId())
          .orElseThrow(() -> new RuntimeException("Team not found"));
      user.setTeam(team);
    }

    userRepository.save(user);

    String token = tokenProvider.generateToken(user.getUsername());

    return AuthDto.AuthResponse.builder()
        .token(token)
        .username(user.getUsername())
        .role(user.getRole().name())
        .userId(user.getId())
        .build();
  }

  public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    String token = tokenProvider.generateToken(authentication);
    User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));

    return AuthDto.AuthResponse.builder()
        .token(token)
        .username(user.getUsername())
        .role(user.getRole().name())
        .userId(user.getId())
        .build();
  }
}
