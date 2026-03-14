package com.andreasik.efipoker.shared.test;

import com.andreasik.efipoker.auth.UserRepository;
import com.andreasik.efipoker.estimation.estimate.EstimateRepository;
import com.andreasik.efipoker.estimation.room.RoomRepository;
import com.andreasik.efipoker.estimation.task.TaskRepository;
import com.andreasik.efipoker.participant.ParticipantRepository;
import com.andreasik.efipoker.project.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("component")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseComponentTest {

  @ServiceConnection static final PostgreSQLContainer postgres;

  static {
    postgres = new PostgreSQLContainer("postgres:17-alpine");
    postgres.start();
  }

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected ProjectRepository projectRepository;

  @Autowired protected RoomRepository roomRepository;

  @Autowired protected TaskRepository taskRepository;

  @Autowired protected ParticipantRepository participantRepository;

  @Autowired protected EstimateRepository estimateRepository;

  @Autowired protected UserRepository userRepository;

  protected String toJson(Object obj) throws Exception {
    return objectMapper.writeValueAsString(obj);
  }

  protected String loginAsTestAdmin() throws Exception {
    // language=JSON
    String body =
        """
        {"username":"testadmin","password":"testpassword"}
        """;
    MvcResult result =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/auth/login")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(body))
            .andReturn();

    String json = result.getResponse().getContentAsString();
    return objectMapper.readTree(json).get("token").asText();
  }
}
