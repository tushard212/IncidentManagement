package com.incidenthub.service;

import com.incidenthub.model.Incident;
import com.incidenthub.model.User;
import com.incidenthub.repository.IncidentRepository;
import com.incidenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("incidentAccessService")
@RequiredArgsConstructor
public class IncidentAccessService {

  private final IncidentRepository incidentRepository;
  private final UserRepository userRepository;

  /**
   * Check if the user is the assignee or the reporter of the incident.
   */
  public boolean isAssignedOrReporter(Long incidentId, String username) {
    Incident incident = incidentRepository.findById(incidentId).orElse(null);
    if (incident == null)
      return false;

    User user = userRepository.findByUsername(username).orElse(null);
    if (user == null)
      return false;

    boolean isAssignee = incident.getAssignee() != null
        && incident.getAssignee().getId().equals(user.getId());
    boolean isReporter = incident.getReporter() != null
        && incident.getReporter().getId().equals(user.getId());
    boolean isTeamMember = incident.getTeam() != null
        && user.getTeam() != null
        && incident.getTeam().getId().equals(user.getTeam().getId());

    return isAssignee || isReporter || isTeamMember;
  }

  /**
   * Check if the requesting user is the same user (for viewing own assigned
   * incidents).
   */
  public boolean isSameUser(Long userId, String username) {
    User user = userRepository.findByUsername(username).orElse(null);
    return user != null && user.getId().equals(userId);
  }

  /**
   * Check if the requesting user belongs to the specified team.
   */
  public boolean isTeamMember(Long teamId, String username) {
    User user = userRepository.findByUsername(username).orElse(null);
    return user != null && user.getTeam() != null && user.getTeam().getId().equals(teamId);
  }
}
