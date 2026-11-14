package org.example.backendweride.platform.shared.infraestructure.persistence.jpa.strategy;

import jakarta.persistence.Table;
import org.example.backendweride.platform.garage.domain.model.entities.VehicleFeature;
import org.example.backendweride.platform.booking.domain.model.entities.BookingIssue;
import org.example.backendweride.platform.plan.domain.model.entities.PlanBenefit;
import org.example.backendweride.platform.location.domain.model.entities.LocationAmenity;
import org.example.backendweride.platform.trip.domain.entities.TripIncidentReport;
import org.example.backendweride.platform.trip.domain.entities.TripPhoto;
import org.example.backendweride.platform.trip.domain.entities.TripRouteCoordinate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JpaTableNamingTest {

    @Test
    void entitiesDeclareStablePluralTableNames() {
        Map<Class<?>, String> expectedNames = Map.of(
                VehicleFeature.class, "vehicle_features",
                BookingIssue.class, "booking_issues",
                PlanBenefit.class, "plan_benefits",
                LocationAmenity.class, "location_amenities",
                TripIncidentReport.class, "trip_incident_reports",
                TripPhoto.class, "trip_photos",
                TripRouteCoordinate.class, "trip_route_coordinates"
        );

        expectedNames.forEach((entityClass, expectedName) ->
                assertEquals(expectedName, entityClass.getAnnotation(Table.class).name(),
                        () -> "Unexpected table name for " + entityClass.getSimpleName()));
    }

    @Test
    void childEntitiesDeclareTheExpectedCollectionTableNames() {
        Map<Class<?>, String> expectedNames = Map.of(
                VehicleFeature.class, "vehicle_features",
                BookingIssue.class, "booking_issues",
                PlanBenefit.class, "plan_benefits",
                LocationAmenity.class, "location_amenities",
                TripIncidentReport.class, "trip_incident_reports",
                TripPhoto.class, "trip_photos",
                TripRouteCoordinate.class, "trip_route_coordinates"
        );

        expectedNames.forEach((entityClass, expectedName) ->
                assertEquals(expectedName, entityClass.getAnnotation(Table.class).name(),
                        () -> "Unexpected table name for " + entityClass.getSimpleName()));
    }
}
