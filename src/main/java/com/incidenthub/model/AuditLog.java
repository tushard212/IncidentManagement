package com.incidenthub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
    @Index(name = "idx_audit_user", columnList = "performedBy"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String entityType; // INCIDENT, TEAM, USER, ON_CALL

  @Column(nullable = false)
  private Long entityId;

  @Column(nullable = false)
  private String action; // CREATE, UPDATE, DELETE, STATUS_CHANGE

  @Column(nullable = false)
  private String performedBy;

  @Column(columnDefinition = "TEXT")
  private String oldValue;

  @Column(columnDefinition = "TEXT")
  private String newValue;

  @Column(columnDefinition = "TEXT")
  private String changes; // JSON diff of what changed

  @Column(nullable = false)
  private String ipAddress;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @PrePersist
  protected void onCreate() {
    if (timestamp == null)
      timestamp = LocalDateTime.now();
  }
}
