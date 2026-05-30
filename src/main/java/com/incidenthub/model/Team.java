package com.incidenthub.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

  @Id
  @GeneratedValue(generator = "snowflake")
  @GenericGenerator(name = "snowflake", type = com.incidenthub.util.SnowflakeIdGenerator.class)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  private String description;

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
  @Builder.Default
  private List<User> members = new ArrayList<>();

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
  @Builder.Default
  private List<EscalationPolicy> escalationPolicies = new ArrayList<>();

  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
