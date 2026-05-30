package com.incidenthub.service;

import com.incidenthub.config.IncidentMetrics;
import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.Incident;
import com.incidenthub.model.User;
import com.incidenthub.model.enums.IncidentStatus;
import com.incidenthub.model.enums.Role;
import com.incidenthub.model.enums.Severity;
import com.incidenthub.repository.*;
import com.incidenthub.websocket.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

  @Mock
  private IncidentRepository incidentRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private TeamRepository teamRepository;
  @Mock
  private IncidentTimelineRepository timelineRepository;
  @Mock
  private WebSocketNotificationService notificationService;
  @Mock
  private AuditService auditService;
  @Mock
  private IncidentMetrics incidentMetrics;

  @InjectMocks
  private IncidentService incidentService;

  private User testUser;
  private Incident testIncident;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .username("tushar")
        .fullName("Tushar Dalmia")
        .role(Role.ENGINEER)
        .build();

    testIncident = Incident.builder()
        .id(100L)
        .title("API Gateway Down")
        .description("Gateway returning 503")
        .severity(Severity.CRITICAL)
        .status(IncidentStatus.OPEN)
        .reporter(testUser)
        .createdAt(LocalDateTime.now())
        .slaDeadline(LocalDateTime.now().plusMinutes(15))
        .build();
  }

  @Test
  @DisplayName("Should create incident successfully")
  void createIncident_Success() {
    IncidentDto.CreateRequest request = IncidentDto.CreateRequest.builder()
        .title("Test Incident")
        .description("Test Description")
        .severity(Severity.HIGH)
        .service("payment-service")
        .build();

    when(userRepository.findByUsername("tushar")).thenReturn(Optional.of(testUser));
    when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
      Incident i = inv.getArgument(0);
      i.setId(101L);
      i.setCreatedAt(LocalDateTime.now());
      return i;
    });
    when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

    IncidentDto.Response response = incidentService.createIncident(request, "tushar");

    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo("Test Incident");
    assertThat(response.getSeverity()).isEqualTo(Severity.HIGH);
    verify(incidentRepository).save(any(Incident.class));
    verify(notificationService).notifyIncidentCreated(any());
    verify(incidentMetrics).recordIncidentCreated();
    verify(auditService).logAction(eq("INCIDENT"), any(), eq("CREATE"), eq("tushar"), any(), any(), any());
  }

  @Test
  @DisplayName("Should throw when reporter not found")
  void createIncident_ReporterNotFound() {
    IncidentDto.CreateRequest request = IncidentDto.CreateRequest.builder()
        .title("Test").severity(Severity.LOW).build();

    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> incidentService.createIncident(request, "unknown"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Reporter not found");
  }

  @Test
  @DisplayName("Should acknowledge incident from OPEN status")
  void acknowledgeIncident_Success() {
    when(incidentRepository.findById(100L)).thenReturn(Optional.of(testIncident));
    when(userRepository.findByUsername("tushar")).thenReturn(Optional.of(testUser));
    when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);
    when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

    IncidentDto.Response response = incidentService.acknowledgeIncident(100L, "tushar");

    assertThat(response).isNotNull();
    assertThat(testIncident.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
    assertThat(testIncident.getAcknowledgedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should fail to acknowledge non-OPEN incident")
  void acknowledgeIncident_InvalidStatus() {
    testIncident.setStatus(IncidentStatus.INVESTIGATING);
    when(incidentRepository.findById(100L)).thenReturn(Optional.of(testIncident));
    when(userRepository.findByUsername("tushar")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> incidentService.acknowledgeIncident(100L, "tushar"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("OPEN status");
  }

  @Test
  @DisplayName("Should update incident status with valid transition")
  void updateStatus_ValidTransition() {
    when(incidentRepository.findById(100L)).thenReturn(Optional.of(testIncident));
    when(userRepository.findByUsername("tushar")).thenReturn(Optional.of(testUser));
    when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);
    when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

    incidentService.updateIncidentStatus(100L, IncidentStatus.ACKNOWLEDGED, "tushar");

    assertThat(testIncident.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
    verify(auditService).logAction(eq("INCIDENT"), eq(100L), eq("STATUS_CHANGE"),
        eq("tushar"), eq("OPEN"), eq("ACKNOWLEDGED"), anyString());
  }

  @Test
  @DisplayName("Should reject invalid status transition")
  void updateStatus_InvalidTransition() {
    testIncident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(100L)).thenReturn(Optional.of(testIncident));
    when(userRepository.findByUsername("tushar")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> incidentService.updateIncidentStatus(100L, IncidentStatus.OPEN, "tushar"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Invalid status transition");
  }

  @Test
  @DisplayName("Should get all incidents paginated")
  void getAllIncidents_Paginated() {
    Page<Incident> page = new PageImpl<>(List.of(testIncident), PageRequest.of(0, 20), 1);
    when(incidentRepository.findAll(any(PageRequest.class))).thenReturn(page);
    when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

    Page<IncidentDto.Response> result = incidentService.getAllIncidents(PageRequest.of(0, 20));

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("API Gateway Down");
  }

  @Test
  @DisplayName("Should get dashboard stats")
  void getDashboardStats_Success() {
    when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(5L);
    when(incidentRepository.countByStatus(IncidentStatus.ACKNOWLEDGED)).thenReturn(2L);
    when(incidentRepository.countByStatus(IncidentStatus.INVESTIGATING)).thenReturn(3L);
    when(incidentRepository.countByStatus(IncidentStatus.RESOLVED)).thenReturn(10L);
    when(incidentRepository.countByStatus(IncidentStatus.CLOSED)).thenReturn(20L);
    when(incidentRepository.countBySeverity()).thenReturn(List.of(
        new Object[] { "CRITICAL", 2L }, new Object[] { "HIGH", 5L }));
    when(incidentRepository.findBreachedIncidents(any(), any())).thenReturn(List.of(testIncident));

    IncidentDto.DashboardStats stats = incidentService.getDashboardStats();

    assertThat(stats.getTotalOpen()).isEqualTo(5);
    assertThat(stats.getTotalResolved()).isEqualTo(10);
    assertThat(stats.getSlaBreachedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should delete incident and timeline entries")
  void deleteIncident_Success() {
    when(incidentRepository.findById(100L)).thenReturn(Optional.of(testIncident));

    incidentService.deleteIncident(100L, "admin");

    verify(timelineRepository).deleteByIncidentId(100L);
    verify(incidentRepository).delete(testIncident);
  }

  @Test
  @DisplayName("Should record resolved metric with duration")
  void resolveIncident_RecordsMetric() {
    testIncident.setStatus(IncidentStatus.INVESTIGATING);
    when(incidentRepository.findById(100L)).thenReturn(Optional.of(testIncident));
    when(userRepository.findByUsername("tushar")).thenReturn(Optional.of(testUser));
    when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);
    when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

    incidentService.updateIncidentStatus(100L, IncidentStatus.RESOLVED, "tushar");

    assertThat(testIncident.getResolvedAt()).isNotNull();
    verify(incidentMetrics).recordIncidentResolved(any());
  }
}
