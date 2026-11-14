package org.example.backendweride.platform.profile.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.profile.domain.model.aggregates.Profile;
import org.example.backendweride.platform.profile.infrastructure.persistence.jpa.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ProfileRepository profileRepository;

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

    @Test
    void getMyProfile_returnsProfileAssociatedWithAuthenticatedAccount() throws Exception {

        String username = "profile_it_01";
        String token = tokenFor(username);
        Long accountId = accountRepository.findByUserName(username).orElseThrow().getId();

        mockMvc.perform(
                        get("/api/v1/profiles/me")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(accountId.intValue()));

        Profile profile = profileRepository.findByUserId(accountId).orElseThrow();

        assertNotNull(profile.getId());
        assertEquals(accountId, profile.getUserId());
    }

    @Test
    void updateMyProfile_persistsUpdatedInformation() throws Exception {

        String username = "profile_it_02";
        String token = tokenFor(username);
        Long accountId = accountRepository.findByUserName(username).orElseThrow().getId();

        mockMvc.perform(
                        put("/api/v1/profiles/me")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                          "firstName": "Ada",
                          "lastName": "Lovelace",
                          "email": "ada.integration@example.test"
                        }
                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada.integration@example.test"));

        Profile persisted = profileRepository.findByUserId(accountId).orElseThrow();

        assertEquals("Ada", persisted.getFirstName());
        assertEquals("Lovelace", persisted.getLastName());
        assertEquals("ada.integration@example.test", persisted.getEmail());
    }

    @Test
    void getProfileById_returnsNotFoundForAnotherUsersProfile() throws Exception {

        String ownerUsername = "profile_owner_it";
        tokenFor(ownerUsername);
        Long ownerAccountId = accountRepository.findByUserName(ownerUsername).orElseThrow().getId();
        Long profileId = profileRepository.findByUserId(ownerAccountId).orElseThrow().getId();

        String otherUserToken = tokenFor("profile_other_it");

        mockMvc.perform(
                        get("/api/v1/profiles/" + profileId)
                                .header("Authorization", "Bearer " + otherUserToken)
                )
                .andExpect(status().isNotFound());
    }
}
