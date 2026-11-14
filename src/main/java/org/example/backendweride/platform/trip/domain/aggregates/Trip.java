package org.example.backendweride.platform.trip.domain.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.backendweride.platform.trip.domain.commands.CreateTripCommand;
import org.example.backendweride.platform.trip.domain.commands.UpdateTripCommand;
import org.example.backendweride.platform.trip.domain.entities.TripIncidentReport;
import org.example.backendweride.platform.trip.domain.entities.TripPhoto;
import org.example.backendweride.platform.trip.domain.entities.TripRouteCoordinate;
import org.example.backendweride.platform.trip.domain.valueobjects.RouteCoordinates;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "trips")
@EntityListeners(AuditingEntityListener.class)
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter
    private Long bookingId;

    @Getter
    private String userId;

    @Getter
    private Long vehicleId;

    @Getter
    private Long startLocationId;

    @Getter
    private Long endLocationId;

    @Getter
    private String route;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripRouteCoordinate> routeCoordinateEntities = new ArrayList<>();

    public List<RouteCoordinates> getRouteCoordinates() {
        return routeCoordinateEntities.stream()
                .map(c -> new RouteCoordinates(c.getLat(), c.getLng()))
                .collect(Collectors.toList());
    }

    @Getter
    private Date startDate;
    @Getter
    private Date endDate;
    @Getter
    private int duration;
    @Getter
    private float distance;
    @Getter
    private float averageSpeed;
    @Getter
    private float maxSpeed;
    @Getter
    private float totalCost;
    @Getter
    private float carbonSaved;
    @Getter
    private int caloriesBurned;
    @Getter
    private String weather;
    @Getter
    private int temperature;
    @Getter
    private String status;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripIncidentReport> incidentReportEntities = new ArrayList<>();

    public List<String> getIncidentReports() {
        return incidentReportEntities.stream().map(TripIncidentReport::getIncidentReport).collect(Collectors.toList());
    }

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripPhoto> photoEntities = new ArrayList<>();

    public List<String> getPhotos() {
        return photoEntities.stream().map(TripPhoto::getPhoto).collect(Collectors.toList());
    }

    protected Trip() {

    }

    public Trip(CreateTripCommand tripCommand) {
        updateFrom(tripCommand);
    }

    public void updateFrom(CreateTripCommand tripCommand) {
        this.userId = tripCommand.userId();
        this.bookingId = tripCommand.bookingId();
        this.vehicleId = tripCommand.vehicleId();
        this.startLocationId = tripCommand.startLocationId();
        this.endLocationId = tripCommand.endLocationId();
        this.route = tripCommand.route();
        this.routeCoordinateEntities = toRouteCoordinateEntities(tripCommand.routeCoordinates());
        this.startDate = tripCommand.startDate();
        this.endDate = tripCommand.endDate();
        this.duration = tripCommand.duration();
        this.distance = tripCommand.distance();
        this.averageSpeed = tripCommand.averageSpeed();
        this.maxSpeed = tripCommand.maxSpeed();
        this.totalCost = tripCommand.totalCost();
        this.carbonSaved = tripCommand.carbonSaved();
        this.caloriesBurned = tripCommand.caloriesBurned();
        this.weather = tripCommand.weather();
        this.temperature = tripCommand.temperature();
        this.status = tripCommand.status();
        this.incidentReportEntities = toIncidentReportEntities(tripCommand.incidentReports());
        this.photoEntities = toPhotoEntities(tripCommand.photos());
    }

    public void updateFrom(UpdateTripCommand command) {
        if (command.bookingId() != null) bookingId = command.bookingId();
        if (command.vehicleId() != null) vehicleId = command.vehicleId();
        if (command.startLocationId() != null) startLocationId = command.startLocationId();
        if (command.endLocationId() != null) endLocationId = command.endLocationId();
        if (command.route() != null) route = command.route();
        if (command.routeCoordinates() != null) {
            routeCoordinateEntities = toRouteCoordinateEntities(
                    command.routeCoordinates()
            );
        }
        if (command.startDate() != null) startDate = command.startDate();
        if (command.endDate() != null) endDate = command.endDate();
        if (command.duration() != null) duration = command.duration();
        if (command.distance() != null) distance = command.distance();
        if (command.averageSpeed() != null) averageSpeed = command.averageSpeed();
        if (command.maxSpeed() != null) maxSpeed = command.maxSpeed();
        if (command.totalCost() != null) totalCost = command.totalCost();
        if (command.carbonSaved() != null) carbonSaved = command.carbonSaved();
        if (command.caloriesBurned() != null) caloriesBurned = command.caloriesBurned();
        if (command.weather() != null) weather = command.weather();
        if (command.temperature() != null) temperature = command.temperature();
        if (command.status() != null) status = command.status();
        if (command.incidentReports() != null) {
            incidentReportEntities = toIncidentReportEntities(
                    command.incidentReports()
            );
        }
        if (command.photos() != null) photoEntities = toPhotoEntities(command.photos());
    }

    private List<TripRouteCoordinate> toRouteCoordinateEntities(List<RouteCoordinates> coordinates) {
        if (coordinates == null) return new ArrayList<>();
        return coordinates.stream()
                .map(c -> new TripRouteCoordinate(this, c.lat(), c.lng()))
                .collect(Collectors.toList());
    }

    private List<TripIncidentReport> toIncidentReportEntities(List<String> reports) {
        if (reports == null) return new ArrayList<>();
        return reports.stream().map(r -> new TripIncidentReport(this, r)).collect(Collectors.toList());
    }

    private List<TripPhoto> toPhotoEntities(List<String> photos) {
        if (photos == null) return new ArrayList<>();
        return photos.stream().map(p -> new TripPhoto(this, p)).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip other = (Trip) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
