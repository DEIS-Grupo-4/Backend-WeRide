package org.example.backendweride.platform.trip.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.trip.infrastructure.persistence.jpa.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TripRepository tripRepository;

    private String tokenFor(String username) throws Exception {

        mockMvc.perform(post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"Strong123!"}""".formatted(username))).andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"Strong123!"}""".formatted(username))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("token").asText();
        assertFalse(token.isBlank(), "El inicio de sesión debe devolver un JWT");
        return token;
    }

    private long createTrip(String token) throws Exception {

        String start = Instant.now().minusSeconds(1800).toString();
        String end = Instant.now().toString();
        String payload = """
                {"bookingId": 7654321,"userId": "user-id-forged-by-client","vehicleId": 101,"startLocationId": 201,"endLocationId": 202,"route": "Integration test route","routeCoordinates": [{"lat": -12.0464, "lng": -77.0428}],"startDate": "%s","endDate": "%s","duration": 30,"distance": 5.5,"averageSpeed": 11.0,"maxSpeed": 20.0,"totalCost": 15.0,"carbonSaved": 0.5,"caloriesBurned": 50,"weather": "sunny","temperature": 22,"status": "completed","incidentReports": ["No incidents"],"photos": ["test/trip-photo.jpg"]}""".formatted(start, end);

        String response = mockMvc.perform(post("/api/v1/trips").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty()).andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        return created.path("id").asLong();
    }

    @Test
    void createTrip_usesAuthenticatedUserAndCanBeListedAndDeleted() throws Exception {

        String username = "trip_it_01";
        String token = tokenFor(username);
        String expectedUserId = accountRepository.findByUserName(username).orElseThrow().getId().toString();
        long tripId = createTrip(token);
        var trip = tripRepository.findById(tripId).orElseThrow();

        assertEquals(expectedUserId, trip.getUserId(), "El controlador debe ignorar el userId enviado por el cliente");

        mockMvc.perform(get("/api/v1/trips").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(tripId)).andExpect(jsonPath("$[0].userId").value(expectedUserId));
        mockMvc.perform(get("/api/v1/trips/" + tripId).header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(tripId));
        mockMvc.perform(delete("/api/v1/trips/" + tripId).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());

        assertTrue(tripRepository.findById(tripId).isEmpty(), "El viaje debe eliminarse de la base de datos");
    }

    @Test
    void getTripById_returnsNotFoundForAnotherUser() throws Exception {

        String ownerToken = tokenFor("trip_owner_it");
        long tripId = createTrip(ownerToken);
        String otherToken = tokenFor("trip_other_it");

        mockMvc.perform(get("/api/v1/trips/" + tripId).header("Authorization", "Bearer " + otherToken)).andExpect(status().isNotFound());
    }
}