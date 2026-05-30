package com.incidenthub.config;

import com.incidenthub.model.*;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.model.enums.Role;
import com.incidenthub.model.enums.Severity;
import com.incidenthub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final IncidentRepository incidentRepository;
  private final EscalationPolicyRepository escalationPolicyRepository;
  private final OnCallScheduleRepository onCallScheduleRepository;
  private final IncidentTimelineRepository timelineRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    if (userRepository.count() > 0)
      return;

    log.info("Initializing demo data...");

    // Create Teams
    Team platformTeam = teamRepository
        .save(Team.builder().name("Platform Engineering").description("Core platform services").build());
    Team backendTeam = teamRepository
        .save(Team.builder().name("Backend Services").description("Backend microservices team").build());

    // Create Users
    User admin = userRepository.save(User.builder()
        .username("admin").email("admin@incidenthub.com")
        .password(passwordEncoder.encode("admin123"))
        .fullName("Admin User").role(Role.ADMIN).team(platformTeam).isOnCall(false).build());

    User manager = userRepository.save(User.builder()
        .username("manager").email("manager@incidenthub.com")
        .password(passwordEncoder.encode("manager123"))
        .fullName("Team Manager").role(Role.MANAGER).team(platformTeam).isOnCall(false).build());

    User engineer1 = userRepository.save(User.builder()
        .username("tushar").email("tushar@incidenthub.com")
        .password(passwordEncoder.encode("tushar123"))
        .fullName("Tushar Dalmia").role(Role.ENGINEER).team(platformTeam).isOnCall(true).build());

    User engineer2 = userRepository.save(User.builder()
        .username("dev2").email("dev2@incidenthub.com")
        .password(passwordEncoder.encode("dev123"))
        .fullName("Developer Two").role(Role.ENGINEER).team(backendTeam).isOnCall(false).build());

    // Create Escalation Policies
    escalationPolicyRepository.save(EscalationPolicy.builder()
        .team(platformTeam).level(1).targetUser(engineer1).escalateAfterMinutes(15).build());
    escalationPolicyRepository.save(EscalationPolicy.builder()
        .team(platformTeam).level(2).targetUser(manager).escalateAfterMinutes(30).build());
    escalationPolicyRepository.save(EscalationPolicy.builder()
        .team(platformTeam).level(3).targetUser(admin).escalateAfterMinutes(60).build());

    // Create On-Call Schedule
    onCallScheduleRepository.save(OnCallSchedule.builder()
        .user(engineer1).team(platformTeam)
        .startTime(LocalDateTime.now().minusHours(2))
        .endTime(LocalDateTime.now().plusHours(10))
        .active(true).build());

    // Create Sample Incidents
    Incident incident1 = incidentRepository.save(Incident.builder()
        .title("Payment Service Down")
        .description("Payment gateway returning 500 errors for all transactions")
        .severity(Severity.CRITICAL)
        .status(IncidentStatus.OPEN)
        .reporter(engineer1).team(platformTeam).service("payment-service")
        .slaBreached(false).escalationLevel(0).build());

    Incident incident2 = incidentRepository.save(Incident.builder()
        .title("High Latency on API Gateway")
        .description("P99 latency increased from 200ms to 2s on the API gateway")
        .severity(Severity.HIGH)
        .status(IncidentStatus.ACKNOWLEDGED)
        .reporter(manager).assignee(engineer1).team(platformTeam).service("api-gateway")
        .acknowledgedAt(LocalDateTime.now().minusMinutes(10))
        .slaBreached(false).escalationLevel(0).build());

    Incident incident3 = incidentRepository.save(Incident.builder()
        .title("Disk Space Warning on DB Server")
        .description("Database server disk usage at 85%")
        .severity(Severity.MEDIUM)
        .status(IncidentStatus.INVESTIGATING)
        .reporter(engineer2).assignee(engineer2).team(backendTeam).service("database")
        .acknowledgedAt(LocalDateTime.now().minusHours(1))
        .slaBreached(false).escalationLevel(0).build());

    // Add timeline entries
    timelineRepository.save(IncidentTimeline.builder()
        .incident(incident1).performedBy(engineer1).action("CREATED")
        .message("Incident created - Payment service returning 500s").build());

    timelineRepository.save(IncidentTimeline.builder()
        .incident(incident2).performedBy(manager).action("CREATED")
        .message("High latency detected on API gateway").build());
    timelineRepository.save(IncidentTimeline.builder()
        .incident(incident2).performedBy(engineer1).action("ACKNOWLEDGED")
        .message("Investigating the root cause").build());

    log.info("Demo data initialized successfully!");
  }
}
