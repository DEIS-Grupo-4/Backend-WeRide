package org.example.backendweride.platform.booking.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.backendweride.platform.booking.infraestructure.persistence.jpa.BookingRepository;
import org.example.backendweride.platform.garage.domain.model.aggregates.Vehicle;
import org.example.backendweride.platform.garage.domain.model.commands.CreateVehicleCommand;
import org.example.backendweride.platform.garage.infrastructure.persistence.jpa.VehicleRepository;
import org.example.backendweride.platform.iam.infrastructure.persistence.jpa.repositories.AccountRepository;
import org.example.backendweride.platform.location.domain.commands.CreateLocationCommand;
import org.example.backendweride.platform.location.domain.model.aggregates.Location;
import org.example.backendweride.platform.location.domain.valueobjects.Coordinates;
import org.example.backendweride.platform.location.domain.valueobjects.OperatingHours;
import org.example.backendweride.platform.location.infrastructure.persistence.jpa.LocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private BookingRepository bookingRepository;

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

    private Long createVehicle(String licensePlate) {

        Vehicle vehicle = new Vehicle(
                new CreateVehicleCommand(
                        "TestBrand",
                        "TestScooter",
                        2026,
                        100,
                        25,
                        35,
                        12.5,
                        "black",
                        licensePlate,
                        "1",
                        "available",
                        "electric_scooter",
                        "1",
                        0.50,
                        "test-vehicle.jpg",
                        List.of(),
                        "good",
                        new Date(),
                        new Date(),
                        0.0,
                        4.5
                )
        );

        return vehicleRepository.saveAndFlush(vehicle).getId();
    }

    private Long createLocation(String name) {

        Location location = new Location(
                new CreateLocationCommand(
                        name,
                        "123 Integration Test Avenue",
                        new Coordinates(-12.0464, -77.0428),
                        "parking",
                        20,
                        20,
                        true,
                        new OperatingHours("06:00", "23:00"),
                        List.of(),
                        "San Isidro",
                        "Ubicación creada para pruebas de integración",
                        "test-location.jpg"
                )
        );

        return locationRepository.saveAndFlush(location).getId();
    }

    private String draftPayload(Long vehicleId, Long startLocationId, Long endLocationId, LocalDateTime startDate) {

        LocalDateTime endDate = startDate.plusMinutes(30);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return """
        {
          "vehicleId": %d,
          "startLocationId": %d,
          "endLocationId": %d,
          "reservedAt": "%s",
          "startDate": "%s",
          "endDate": "%s",
          "paymentMethod": "credit_card",
          "distance": 3.5,
          "duration": 30,
          "averageSpeed": 7.0,
          "rating": {
            "score": 5,
            "comment": "Prueba de integración"
          }
        }
        """.formatted(
                vehicleId,
                startLocationId,
                endLocationId,
                LocalDateTime.now().format(formatter),
                startDate.format(formatter),
                endDate.format(formatter)
        );
    }

    @Test
    void saveDraft_persistsDraftAndOwnerCanDeleteIt() throws Exception {

        // Arrange
        String username = "booking_it_01";
        String token = tokenFor(username);
        Long vehicleId = createVehicle("BOOK-IT-01");
        Long startLocationId = createLocation("Booking Start");
        Long endLocationId = createLocation("Booking End");

        Long authenticatedUserId = accountRepository.findByUserName(username).orElseThrow().getId();
        LocalDateTime startDate = LocalDateTime.now().plusDays(2).withNano(0);

        String response = mockMvc.perform(
                        post("/api/v1/bookings/draft")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        draftPayload(
                                                vehicleId,
                                                startLocationId,
                                                endLocationId,
                                                startDate
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.intValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        Long bookingId = created.path("bookingId").asLong();

        assertTrue(bookingId > 0, "La reserva debe tener un bookingId generado");
        assertEquals(
                authenticatedUserId.longValue(),
                created.path("userId").asLong(),
                "El usuario debe obtenerse del token autenticado"
        );
        assertTrue(
                bookingRepository.findByBookingId(bookingId).isPresent(),
                "El borrador debe quedar guardado en la base de datos"
        );

        // Act
        mockMvc.perform(
                delete("/api/v1/bookings/draft/" + bookingId)
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isOk());

        // Assert
        bookingRepository.flush();

        assertFalse(
                bookingRepository.findByBookingId(bookingId).isPresent(),
                "El borrador eliminado no debe seguir persistido"
        );
    }
}