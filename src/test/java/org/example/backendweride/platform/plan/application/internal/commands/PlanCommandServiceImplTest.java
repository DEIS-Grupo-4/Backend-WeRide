package org.example.backendweride.platform.plan.application.internal.commands;

import org.example.backendweride.platform.plan.domain.commands.CreatePlanCommand;
import org.example.backendweride.platform.plan.domain.model.aggregates.Plan;
import org.example.backendweride.platform.plan.infrastructure.persistence.jpa.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanCommandServiceImplTest {

    @Mock private PlanRepository planRepository;

    @InjectMocks private PlanCommandServiceImpl service;

    private static CreatePlanCommand sampleCommand(String name, float price) {
        return new CreatePlanCommand(name, "Plan de pruebas", price, "PEN", 0.50f, "monthly", 30, 2, 60, 10, 0, List.of("Uso de vehículos"), "#18FA3A", false, true);
    }

    @Test
    void createPlan_verifyValidAndReturnsPlan() {

        // Arrange
        var command = sampleCommand("Premium", 29.99f);

        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Premium", result.get().getName());
        assertEquals(29.99f, result.get().getPrice());
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void deletePlan_returnsTrueAndDeletesPlan() {

        // Arrange
        var planId = 1L;
        when(planRepository.existsById(planId)).thenReturn(true);

        // Act
        var result = service.handle(planId);

        // Assert
        assertTrue(result);
        verify(planRepository).deleteById(planId);
    }

    @Test
    void deletePlan_returnsFalseAndNotDeletes() {

        // Arrange
        var planId = 99L;
        when(planRepository.existsById(planId)).thenReturn(false);

        // Act
        var result = service.handle(planId);

        // Assert
        assertFalse(result);
        verify(planRepository).existsById(planId);
        verify(planRepository, never()).deleteById(anyLong());
    }

    @Test
    void updatePlan_changesAndKeepPlan () {

        // Arrange
        var planId = 1L;
        var plan = new Plan(sampleCommand("Basic", 9.99f));
        var updated = sampleCommand("Premium", 29.99f);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(plan)).thenReturn(plan);

        // Act
        var result = service.handle(planId, updated);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Premium", result.get().getName());
        assertEquals(29.99f, result.get().getPrice());
        verify(planRepository).save(plan);
    }

    @Test
    void updateMissingPlan_notChangesAndNotSaves () {

        // Arrange
        var planId = 99L;
        var updated = sampleCommand("Premium", 29.99f);

        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        // Act
        var result = service.handle(planId, updated);

        // Assert
        assertTrue(result.isEmpty());
        verify(planRepository, never()).save(any(Plan.class));
    }
}
