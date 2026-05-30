package com.incidenthub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidenthub.dto.AuthDto;
import com.incidenthub.dto.IncidentDto;
import com.incidenthub.model.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the full request/response cycle.
 * Uses H2 in-memory database with test profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class IncidentIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  private String adminToken;

  @BeforeEach
  void setUp() throws Exception {
    // Login as admin to get JWT token
    AuthDto.LoginRequest loginRequest = new AuthDto.LoginRequest();
    loginRequest.setUsername("admin");
    loginRequest.setPassword("admin123");

    MvcResult result = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    adminToken = objectMapper.readTree(responseJson).get("token").asText();
  }

  @Test
  @DisplayName("Should login and get JWT token")
  void login_Success() throws Exception {
    AuthDto.LoginRequest request = new AuthDto.LoginRequest();
    request.setUsername("admin");
    request.setPassword("admin123");

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  @DisplayName("Should reject invalid credentials")
  void login_InvalidCredentials() throws Exception {
    AuthDto.LoginRequest request = new AuthDto.LoginRequest();
    request.setUsername("admin");
    request.setPassword("wrong");

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should create and retrieve incident")
  void createAndGetIncident() throws Exception {
    IncidentDto.CreateRequest createRequest = IncidentDto.CreateRequest.builder()
        .title("Integration Test Incident")
        .description("Testing full flow")
        .severity(Severity.HIGH)
        .service("test-service")
        .build();

    MvcResult createResult = mockMvc.perform(post("/api/incidents")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Integration Test Incident"))
        .andExpect(jsonPath("$.severity").value("HIGH"))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andReturn();

    Long incidentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
        .get("id").asLong();

    // Retrieve it
    mockMvc.perform(get("/api/incidents/" + incidentId)
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Integration Test Incident"))
        .andExpect(jsonPath("$.timeline", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Should get paginated incidents list")
  void getIncidents_Paginated() throws Exception {
    mockMvc.perform(get("/api/incidents?page=0&size=10")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").isNumber())
        .andExpect(jsonPath("$.totalPages").isNumber());
  }

  @Test
  @DisplayName("Should get dashboard stats")
  void getDashboardStats() throws Exception {
    mockMvc.perform(get("/api/incidents/dashboard/stats")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalOpen").isNumber())
        .andExpect(jsonPath("$.severityCounts").isArray());
  }

  @Test
  @DisplayName("Should acknowledge an open incident")
  void acknowledgeIncident() throws Exception {
    // Create an incident first
    IncidentDto.CreateRequest req = IncidentDto.CreateRequest.builder()
        .title("Ack Test").severity(Severity.MEDIUM).build();

    MvcResult res = mockMvc.perform(post("/api/incidents")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andReturn();

    Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/incidents/" + id + "/acknowledge")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
  }

  @Test
  @DisplayName("Should return 401 for unauthenticated requests")
  void unauthenticated_Returns401() throws Exception {
    mockMvc.perform(get("/api/incidents"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should get analytics via v2 API")
  void getAnalytics_V2() throws Exception {
    mockMvc.perform(get("/api/v2/analytics?days=30")
        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mttrMinutes").isNumber())
        .andExpect(jsonPath("$.mttaMinutes").isNumber())
        .andExpect(jsonPath("$.slaComplianceRate").isNumber())
        .andExpect(jsonPath("$.periodDays").value(30));
  }

  @Test
  @DisplayName("Actuator health endpoint is public")
  void actuatorHealth_Public() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
