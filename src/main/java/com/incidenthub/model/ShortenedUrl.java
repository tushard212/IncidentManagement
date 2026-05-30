package com.incidenthub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shortened_urls", indexes = {
    @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenedUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 10)
  private String shortCode;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @Column(nullable = false)
  private String createdBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private Long clickCount;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (clickCount == null)
      clickCount = 0L;
  }
}
