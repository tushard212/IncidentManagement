package com.incidenthub.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class TeamDto {

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    private String name;
    private String description;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long id;
    private String name;
    private String description;
    private int memberCount;
    private List<MemberResponse> members;
    private LocalDateTime createdAt;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MemberResponse {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private boolean isOnCall;
  }
}
