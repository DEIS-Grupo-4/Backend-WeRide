package org.example.backendweride.platform.iam.infrastructure.auth;

import org.apache.coyote.http11.upgrade.UpgradeServletOutputStream;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.plan.infrastructure.persistence.jpa.PlanRepository;
import org.example.backendweride.platform.plan.interfaces.resources.PlanResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@Transactional
public class WeRideCoreIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRepository accountRepository;
    @Autowired PlanRepository planRepository;

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + username + "\",\"password\":\"Strong123!\"}")).andExpect(status().isCreated());
    }

    private String jwt(String username) throws Exception {
        register(username);
        var response = mockMvc.perform(post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + username + "\",\"password\":\"Strong123!\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String token = json.path("token").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private String planJson() {
        return """ 
                { "name": "Premium QA", "description": "Plan de integración", "price": 29.99, "currency": "PEN", "pricePerMinute": 0.5, "duration": "monthly", "durationDays": 30, "maxTipsPerDay": 2, "maxMinutesPerTrip": 60, "freeMinutesPerMonth": 10, "discountPercentage": 0, "benefits": ["Reserva rápida"], "color": "#18FA3A", "isPopular": false, "isActive": true } """;
    }

    @Test
    void signUp_keepAccountAndProfile() throws Exception {

        // Arrange
        var username = "iamit01";

        // Act
        register(username);

        // Assert
        var account = accountRepository.findByUserName(username).orElseThrow();
        assertNotNull(account.getId());
        assertNotNull(account.getProfileId());
        assertNotNull(account.getProfileId().profileId());
    }

    @Test
    void signIn_returns200AndJwt() throws Exception {

        // Arrange
        var username = "iamit02";

        // Act
        var token = jwt(username);

        // Assert
        assertFalse(token.isBlank());
    }

    @Test
    void createPlan_return201 () throws Exception {

        // Arrange
        var jwt = jwt("planit01");
        var payload = planJson();

        // Act
        var result = mockMvc.perform(post("/api/v1/plans").header("Authorization", "Bearer" + jwt).contentType(MediaType.APPLICATION_JSON).content(payload));

        // Assert
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Premium QA"));

        assertTrue(planRepository.findAll().stream().anyMatch(p -> "Premium QA".equals(p.getName())));
    }

    @Test
    void getPlan_returnsPlan() throws Exception {

        // Arrange
        var jwt = jwt("planit02");
        mockMvc.perform(post("/api/v1/plans").header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON).content(planJson())).andExpect(status().isCreated());

        // Act
        var result = mockMvc.perform(get("/api/v1/plans").header("Authorization", "Bearer " + jwt));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$[?(@.name == 'Premium QA')]").isNotEmpty());
    }


}
