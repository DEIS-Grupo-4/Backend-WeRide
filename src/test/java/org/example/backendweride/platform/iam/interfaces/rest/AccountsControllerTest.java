package org.example.backendweride.platform.iam.interfaces.rest;

import org.example.backendweride.platform.iam.application.internal.outboundservices.security.AuthenticatedAccountProvider;
import org.example.backendweride.platform.iam.domain.services.AccountQueryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccountsControllerTest {

    private final AccountQueryService accountQueryService = mock(AccountQueryService.class);
    private final AuthenticatedAccountProvider authenticatedAccountProvider = mock(AuthenticatedAccountProvider.class);

    private final AccountsController controller =
            new AccountsController(
                    accountQueryService,
                    authenticatedAccountProvider
            );

    @Test
    void getAccountById_returnsNotFound_whenRequestingAnotherAccount() {
        when(authenticatedAccountProvider.getCurrentAccountId()).thenReturn(42L);

        var response = controller.getAccountById(999L);

        assertEquals(404, response.getStatusCode().value());
        verifyNoInteractions(accountQueryService);
    }
}
