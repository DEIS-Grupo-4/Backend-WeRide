package org.example.backendweride.platform.iam.application.internal.commandservices;

import org.example.backendweride.platform.iam.application.internal.outboundservices.hashing.HashingService;
import org.example.backendweride.platform.iam.application.internal.outboundservices.tokens.TokenService;
import org.example.backendweride.platform.iam.domain.model.aggregates.Account;
import org.example.backendweride.platform.iam.domain.model.commands.SignInCommand;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.profile.interfaces.acl.ProfileContextFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCommandServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private HashingService hashingService;
    @Mock private TokenService tokenService;
    @Mock private ProfileContextFacade profileContextFacade;

    @InjectMocks private AccountCommandServiceImpl service;

    @Test
    void signInWithWrongPassword_throwsAndNeverGeneratesToken() {

        // Arrange
        var account = new Account("auditor@weride.com", "hashed-password");
        var command = new SignInCommand("auditor@weride.com", "malo");

        when(accountRepository.existsByUserName("auditor@weride.com")).thenReturn(true);
        when(accountRepository.findByUserName("auditor@weride.com")).thenReturn(Optional.of(account));
        when(hashingService.matches("malo", "hashed-password")).thenReturn(false);

        // Act
        var exception = assertThrows(RuntimeException.class, () -> service.handle(command));

        // Assert
        assertEquals("Invalid credentials", exception.getMessage());
        verify(tokenService, never()).generateToken(anyString());
    }

    @Test
    void signInWithCorrectPassword_returnsAccountAndToken() {

        // Arrange
        var account = new Account("auditor@weride.com", "hashed-password");
        var command = new SignInCommand("auditor@weride.com", "correct-password");

        when(accountRepository.existsByUserName("auditor@weride.com")).thenReturn(true);
        when(accountRepository.findByUserName("auditor@weride.com")).thenReturn(Optional.of(account));
        when(hashingService.matches("correct-password", "hashed-password")).thenReturn(true);
        when(tokenService.generateToken("auditor@weride.com")).thenReturn("jwt-token-123");

        // Act
        var result = service.handle(command);

        // Assert
        assertTrue(result.isPresent());
        assertSame(account, result.get().getLeft());
        assertEquals("jwt-token-123", result.get().getRight());
    }
}
