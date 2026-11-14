package org.example.backendweride.platform.plan.interfaces;

import org.example.backendweride.platform.plan.domain.services.commands.PlanCommandService;
import org.example.backendweride.platform.plan.domain.services.queries.PlanQueryService;
import org.example.backendweride.platform.plan.interfaces.resources.PlanResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    @Mock
    private PlanCommandService planCommandService;

    @Mock
    private PlanQueryService planQueryService;

    @InjectMocks
    private PlanController controller;

    @Test
    void findAllPlans_returnsPlanResources() {

        // Arrange
        PlanResource plan = new PlanResource(
                1L,
                "Premium",
                "Plan de prueba",
                29.99f,
                "PEN",
                0.5f,
                "monthly",
                30,
                2,
                60,
                10,
                0,
                List.of("Reserva rápida"),
                "#18FA3A",
                false,
                true
        );
        when(planQueryService.handle()).thenReturn(Optional.of(List.of(plan)));

        // Act
        var response = controller.findAllPlans();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(plan, response.getBody().get(0));
        assertEquals("Premium", response.getBody().get(0).name());
        assertEquals(29.99f, response.getBody().get(0).price());

        verify(planQueryService).handle();
    }

    @Test
    void findAllPlans_returnsOk_whenListIsEmpty() {

        // Arrange
        when(planQueryService.handle()).thenReturn(Optional.of(List.of()));

        // Act
        var response = controller.findAllPlans();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(planQueryService).handle();
    }

    @Test
    void findAllPlans_returnsNotFound_whenServiceReturnsEmptyOptional() {

        // Arrange
        when(planQueryService.handle()).thenReturn(Optional.empty());

        // Act
        var response = controller.findAllPlans();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(planQueryService).handle();
    }
}
