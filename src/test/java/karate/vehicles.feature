Feature: Vehicle Fleet Management (Garage)
  As an authenticated WeRide user
  I want to manage vehicles in the fleet
  So that users can browse and book available vehicles

  Background:
    * url baseUrl
    * def randomUser = 'ku' + java.util.UUID.randomUUID().toString().substring(0, 8)
    Given path '/api/v1/authentication/sign-up'
    And request { username: '#(randomUser)', password: 'Weride2026!' }
    When method post
    Then status 201

    Given path '/api/v1/authentication/sign-in'
    And request { username: '#(randomUser)', password: 'Weride2026!' }
    When method post
    Then status 200
    * def authHeader = 'Bearer ' + response.token
    * configure headers = { Authorization: '#(authHeader)' }

  Scenario: Create a vehicle and then retrieve it by id
    Given path '/api/v1/vehicles'
    And request
    """
    {
      "brand": "Xiaomi",
      "model": "Mi Electric Scooter 4",
      "year": 2026,
      "battery": 100,
      "maxSpeed": 25,
      "range": 30,
      "weight": 14.5,
      "color": "Black",
      "licensePlate": "KAR-001",
      "location": "San Isidro, Lima",
      "status": "AVAILABLE",
      "type": "SCOOTER",
      "companyId": "weride-fleet-01",
      "pricePerMinute": 0.35,
      "image": "https://example.com/scooter.png",
      "features": ["GPS", "Bluetooth Lock"],
      "maintenanceStatus": "OK",
      "totalKilometers": 0,
      "rating": 5.0
    }
    """
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.brand == 'Xiaomi'
    * def createdVehicleId = response.id

    Given path '/api/v1/vehicles', createdVehicleId
    When method get
    Then status 200
    And match response.licensePlate == 'KAR-001'

  Scenario: List all vehicles returns an array
    Given path '/api/v1/vehicles'
    When method get
    Then status 200
    And match response == '#array'

  Scenario: Requesting a non-existent vehicle returns 404
    Given path '/api/v1/vehicles', '999999999'
    When method get
    Then status 404

  Scenario: Create and delete a vehicle (lifecycle)
    Given path '/api/v1/vehicles'
    And request
      """
      {
        "brand": "Segway",
        "model": "Ninebot Max",
        "year": 2025,
        "battery": 90,
        "maxSpeed": 20,
        "range": 25,
        "weight": 18.7,
        "color": "Grey",
        "licensePlate": "KAR-002",
        "location": "Miraflores, Lima",
        "status": "AVAILABLE",
        "type": "SCOOTER",
        "companyId": "weride-fleet-01",
        "pricePerMinute": 0.30,
        "image": "https://example.com/segway.png",
        "features": ["GPS"],
        "maintenanceStatus": "OK",
        "totalKilometers": 0,
        "rating": 4.8
      }
      """
    When method post
    Then status 201
    * def vehicleId = response.id

    Given path '/api/v1/vehicles', vehicleId
    When method get
    Then status 200

    Given path '/api/v1/vehicles', vehicleId
    When method delete
    Then status 204

    Given path '/api/v1/vehicles', vehicleId
    When method get
    Then status 404