package org.example.backendweride.platform.notifications.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.notifications.infrastructure.persistence.jpa.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private NotificationRepository notificationRepository;

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

    private String createNotification(String token) throws Exception {

        String payload = """
          {
          "title": "Integration test notification",
          "message": "Notification created through MockMvc",
          "type": "info",
          "category": "integration-test",
          "priority": "normal",
          "relatedEntityId": "booking-it-01",
          "relatedEntityType": "booking",
          "icon": "bell",
          "color": "blue"
        }
        """;

        String response = mockMvc.perform(
                        post("/api/v1/notifications")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration test notification"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        String publicId = created.path("id").asText();
        assertFalse(publicId.isBlank(), "La notificación debe devolver su identificador público");
        return publicId;
    }

    @Test
    void createNotification_isPersistedAndCanBeMarkedAsRead() throws Exception {

        String username = "notification_it_01";
        String token = tokenFor(username);
        String expectedUserId = accountRepository.findByUserName(username).orElseThrow().getId().toString();
        String publicId = createNotification(token);
        var notification = notificationRepository.findByPublicId(publicId).orElseThrow();

        assertEquals(expectedUserId, notification.getUserId());
        assertFalse(notification.isRead());

        mockMvc.perform(
                        get("/api/v1/notifications")
                                .queryParam("userId", "someone-else")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(publicId));

        mockMvc.perform(
                        patch("/api/v1/notifications/" + publicId + "/read")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());

        var updated = notificationRepository.findByPublicId(publicId).orElseThrow();
        assertTrue(updated.isRead());
        assertNotNull(updated.getReadAt());
    }

    @Test
    void getAndDeleteNotification_returnNotFoundForAnotherUser() throws Exception {

        String ownerToken = tokenFor("notif_owner_it");
        String publicId = createNotification(ownerToken);
        String otherToken = tokenFor("notif_other_it");

        mockMvc.perform(
                        get("/api/v1/notifications/" + publicId)
                                .header("Authorization", "Bearer " + otherToken)
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        delete("/api/v1/notifications/" + publicId)
                                .header("Authorization", "Bearer " + otherToken)
                )
                .andExpect(status().isNotFound());

        assertTrue(
                notificationRepository.findByPublicId(publicId).isPresent(),
                "La notificación del propietario no debe eliminarse por una petición de otro usuario"
        );
    }
}



