package org.example.backendweride.platform.travelhistory.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.travelhistory.infrastructure.persistence.jpa.TravelHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TravelHistoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TravelHistoryRepository travelHistoryRepository;

    private String tokenFor(String username) throws Exception {

        mockMvc.perform(post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"Strong123!"}""".formatted(username))).andExpect(status().isCreated());

        String response = mockMvc.perform(
                        post("/api/v1/authentication/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"username":"%s","password":"Strong123!"}
                        """.formatted(username))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).path("token").asText();
        assertFalse(token.isBlank(), "El inicio de sesión debe devolver un JWT");
        return token;
    }

    private String historyPayload(String location, String vehicle) {

        return """
        {
          "location": "%s",
          "vehicle": "%s",
          "image": "test/travel-history.jpg",
          "tripDuration": "25 min",
          "travelDistance": "4.2 km"
        }
        """.formatted(location, vehicle);
    }

    private long createHistory(String token) throws Exception {

        String response = mockMvc.perform(
                        post("/api/v1/travel-history")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(historyPayload("San Isidro", "Test Scooter"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location").value("San Isidro"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        return created.path("id").asLong();
    }

    @Test
    void createTravelHistory_isPersistedAndReturnedByMeEndpoint() throws Exception {

        String username = "history_it_01";
        String token = tokenFor(username);
        Long accountId = accountRepository.findByUserName(username).orElseThrow().getId();
        long historyId = createHistory(token);

        assertTrue(travelHistoryRepository.findById(historyId).isPresent());
        assertEquals(1, travelHistoryRepository.findByUserId(accountId).size());

        mockMvc.perform(
                        get("/api/v1/travel-history/me")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(historyId))
                .andExpect(jsonPath("$[0].userId").value(accountId.intValue()))
                .andExpect(jsonPath("$[0].location").value("San Isidro"));

        mockMvc.perform(
                        put("/api/v1/travel-history/" + historyId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(historyPayload("Miraflores", "Updated Test Scooter"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Miraflores"))
                .andExpect(jsonPath("$.vehicle").value("Updated Test Scooter"));

        var persisted = travelHistoryRepository.findById(historyId).orElseThrow();

        assertEquals("Miraflores", persisted.getLocation());
        assertEquals("Updated Test Scooter", persisted.getVehicle());
    }

    @Test
    void otherUserCannotReadOrUpdateAnotherUsersTravelHistory() throws Exception {

        String ownerUsername = "history_owner_it";
        String ownerToken = tokenFor(ownerUsername);
        Long ownerAccountId = accountRepository.findByUserName(ownerUsername).orElseThrow().getId();
        long historyId = createHistory(ownerToken);
        String otherToken = tokenFor("history_other_it");

        mockMvc.perform(
                        get("/api/v1/travel-history/" + ownerAccountId)
                                .header("Authorization", "Bearer " + otherToken)
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        put("/api/v1/travel-history/" + historyId)
                                .header("Authorization", "Bearer " + otherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(historyPayload("Unauthorized change", "Other vehicle"))
                )
                .andExpect(status().isNotFound());

        var persisted = travelHistoryRepository.findById(historyId).orElseThrow();

        assertEquals(ownerAccountId, persisted.getUserId());
        assertEquals("San Isidro", persisted.getLocation());
    }
}
