package com.incidenthub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String recipientEmail;

  @Column(nullable = false)
  private String recipientName;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  private Long incidentId;

  private String incidentTitle;

  @Column(nullable = false)
  private LocalDateTime sentAt;

  private String errorMessage;

  public enum NotificationType {
    INCIDENT_CREATED,
    INCIDENT_ESCALATED,
    INCIDENT_RESOLVED
  }

  public enum NotificationStatus {
    SENT,
    FAILED
  }
}
