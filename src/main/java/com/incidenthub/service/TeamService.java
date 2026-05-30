package com.incidenthub.service;

import com.incidenthub.dto.TeamDto;
import com.incidenthub.model.Team;
import com.incidenthub.model.User;
import com.incidenthub.repository.TeamRepository;
import com.incidenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;

  @Transactional
  public TeamDto.Response createTeam(TeamDto.CreateRequest request) {
    if (teamRepository.existsByName(request.getName())) {
      throw new RuntimeException("Team name already exists");
    }

    Team team = Team.builder()
        .name(request.getName())
        .description(request.getDescription())
        .build();

    team = teamRepository.save(team);
    return mapToResponse(team);
  }

  public List<TeamDto.Response> getAllTeams() {
    return teamRepository.findAll().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public TeamDto.Response getTeam(Long id) {
    Team team = teamRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Team not found"));
    return mapToResponse(team);
  }

  @Transactional
  public TeamDto.Response addMember(Long teamId, Long userId) {
    Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new RuntimeException("Team not found"));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    user.setTeam(team);
    userRepository.save(user);

    return mapToResponse(team);
  }

  private TeamDto.Response mapToResponse(Team team) {
    List<TeamDto.MemberResponse> members = team.getMembers().stream()
        .map(m -> TeamDto.MemberResponse.builder()
            .id(m.getId())
            .username(m.getUsername())
            .fullName(m.getFullName())
            .role(m.getRole().name())
            .isOnCall(m.isOnCall())
            .build())
        .collect(Collectors.toList());

    return TeamDto.Response.builder()
        .id(team.getId())
        .name(team.getName())
        .description(team.getDescription())
        .memberCount(team.getMembers().size())
        .members(members)
        .createdAt(team.getCreatedAt())
        .build();
  }
}
