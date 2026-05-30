package com.incidenthub.websocket;

import com.incidenthub.dto.IncidentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

  private final SimpMessagingTemplate messagingTemplate;

  public void notifyIncidentCreated(IncidentDto.Response incident) {
    messagingTemplate.convertAndSend("/topic/incidents", new NotificationPayload("INCIDENT_CREATED", incident));
  }

  public void notifyIncidentUpdated(IncidentDto.Response incident) {
    messagingTemplate.convertAndSend("/topic/incidents", new NotificationPayload("INCIDENT_UPDATED", incident));
  }

  public void notifyEscalation(IncidentDto.Response incident) {
    messagingTemplate.convertAndSend("/topic/incidents", new NotificationPayload("INCIDENT_ESCALATED", incident));
  }

  public void notifySlaBreached(IncidentDto.Response incident) {
    messagingTemplate.convertAndSend("/topic/incidents", new NotificationPayload("SLA_BREACHED", incident));
  }

  public record NotificationPayload(String type, Object data) {
  }
}
