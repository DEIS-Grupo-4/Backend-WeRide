package org.example.backendweride.platform.iam.infrastructure.auth;

import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
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
public class AuthenticationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRepository accountRepository;

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

    @Test
    void signUp_createsAccountAndAssociatedProfile() throws Exception {

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
}
