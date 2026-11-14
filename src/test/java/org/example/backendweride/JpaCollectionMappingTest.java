package org.example.backendweride;

import org.example.backendweride.platform.booking.domain.model.aggregates.Booking;
import org.example.backendweride.platform.booking.domain.model.commands.CreateBookingCommand;
import org.example.backendweride.platform.garage.domain.model.aggregates.Vehicle;
import org.example.backendweride.platform.garage.domain.model.commands.CreateVehicleCommand;
import org.example.backendweride.platform.garage.infrastructure.persistence.jpa.VehicleRepository;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the auxiliary tables generated from JPA collections.
 *
 * <p>When the mappings were {@code @ElementCollection}, Hibernate generated tables
 * (vehicle_features, booking_issues, ...) without a PRIMARY KEY, which MySQL rejects
 * when {@code sql_require_primary_key = ON} (the default in Render's managed MySQL).
 * These tests verify the new explicit child entities persist, reload and update correctly.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JpaCollectionMappingTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void vehicleFeaturesPersistReloadAndUpdate() {
        Vehicle vehicle = new Vehicle(new CreateVehicleCommand(
                "Xiaomi", "M365", 2023, 82, 25, 30, 12.5, "black", "XM001", "1",
                "available", "electric_scooter", "1", 0.5, "assets/vehicles/xiaomi-m365.jpg",
                List.of("GPS", "Bluetooth", "Mobile App"), "good",
                new Date(), new Date(), 1250.5, 4.6));
        vehicleRepository.save(vehicle);

        Vehicle loaded = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(List.of("GPS", "Bluetooth", "Mobile App"), loaded.getFeatures());

        loaded.updateInformation(
                new org.example.backendweride.platform.garage.domain.model.commands.UpdateVehicleCommand(
                loaded.getId(), "Xiaomi", "M365", 2023, 82, 25, 30, 12.5, "black", "XM001", "1",
                "available", "electric_scooter", "1", 0.5, "assets/vehicles/xiaomi-m365.jpg",
                List.of("GPS", "LED Lights"), "good", new Date(), new Date(), 1250.5, 4.6));
        vehicleRepository.save(loaded);

        Vehicle reloaded = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(List.of("GPS", "LED Lights"), reloaded.getFeatures());
    }

    @Test
    void bookingIssuesPersistAndReload() {
        Booking booking = Booking.createConfirmedFrom(new CreateBookingCommand(
                42L, 7L, 1L, 2L,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                null, null, "confirmed",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                new BigDecimal("10.00"),
                "credit_card", "paid", 5.0, 30, 10.0,
                null, null));
        booking.addIssue("Late pickup");
        booking.addIssue("Damaged scooter");
        bookingRepository.save(booking);

        Booking loaded = bookingRepository.findById(booking.getId()).orElseThrow();
        assertTrue(loaded.getIssues().contains("Late pickup"));
        assertTrue(loaded.getIssues().contains("Damaged scooter"));
    }
}
