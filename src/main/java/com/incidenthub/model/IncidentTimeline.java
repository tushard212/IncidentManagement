package com.incidenthub.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_timeline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentTimeline {

  @Id
  @GeneratedValue(generator = "snowflake")
  @GenericGenerator(name = "snowflake", type = com.incidenthub.util.SnowflakeIdGenerator.class)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User performedBy;

  @Column(nullable = false)
  private String action; // e.g., "CREATED", "ACKNOWLEDGED", "ESCALATED", "RESOLVED"

  @Column(length = 1000)
  private String message;

  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
