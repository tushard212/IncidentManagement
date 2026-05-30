package com.incidenthub.dto;

import lombok.*;

import java.time.LocalDateTime;

public class OnCallDto {

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    private Long userId;
    private Long teamId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long id;
    private String userName;
    private Long userId;
    private String teamName;
    private Long teamId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean active;
  }
}
