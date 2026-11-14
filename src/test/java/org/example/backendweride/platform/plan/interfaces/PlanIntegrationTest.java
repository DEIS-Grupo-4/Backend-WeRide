package org.example.backendweride.platform.plan.interfaces;

import org.example.backendweride.platform.plan.infrastructure.persistence.jpa.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PlanIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlanRepository planRepository;

    private void register(String username) throws Exception {
        String credentials = "{\"username\":\"" + username
                + "\",\"password\":\"Strong123!\"}";

        mockMvc.perform(
                post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials)
        ).andExpect(status().isCreated());
    }

    private String jwt(String username) throws Exception {
        register(username);

        String credentials = "{\"username\":\"" + username
                + "\",\"password\":\"Strong123!\"}";

        var response = mockMvc.perform(
                        post("/api/v1/authentication/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(credentials)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String token = json.path("token").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private String planJson() {
        return """
            {
              "name": "Premium QA",
              "description": "Plan de integración",
              "price": 29.99,
              "currency": "PEN",
              "pricePerMinute": 0.5,
              "duration": "monthly",
              "durationDays": 30,
              "maxTripsPerDay": 2,
              "maxMinutesPerTrip": 60,
              "freeMinutesPerMonth": 10,
              "discountPercentage": 0,
              "benefits": ["Reserva rápida"],
              "color": "#18FA3A",
              "isPopular": false,
              "isActive": true
            }
            """;
    }

    @Test
    void createPlan_return201 () throws Exception {

        // Arrange
        var jwt = jwt("planit01");
        var payload = planJson();

        // Act
        var result = mockMvc.perform(
                post("/api/v1/plans")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
        );

        // Assert
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Premium QA"));

        assertTrue(planRepository.findAll().stream().anyMatch(p -> "Premium QA".equals(p.getName())));
    }

    @Test
    void getPlan_returnsPlan() throws Exception {

        // Arrange
        var jwt = jwt("planit02");
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(planJson())
                )
                .andExpect(status().isCreated());

        // Act
        var result = mockMvc.perform(get("/api/v1/plans").header("Authorization", "Bearer " + jwt));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$[?(@.name == 'Premium QA')]").isNotEmpty());
    }


}
