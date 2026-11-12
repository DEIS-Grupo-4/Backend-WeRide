Feature: Authentication - Sign Up & Sign In
  As a WeRide user
  I want to create an account and sign in
  So that I can access the booking and vehicle features of the platform

  Background:
    * url baseUrl
    * def randomUsername = 'ku' + java.util.UUID.randomUUID().toString().substring(0, 8)

  Scenario: Sign up with valid credentials creates a new account
    Given path '/api/v1/authentication/sign-up'
    And request { username: '#(randomUsername)', password: 'Weride2026!' }
    When method post
    Then status 201
    And match response.username == randomUsername
    And match response.id == '#number'

  Scenario: Sign up with a blank username is rejected
    Given path '/api/v1/authentication/sign-up'
    And request { username: '', password: 'Weride2026!' }
    When method post
    Then status 400

  Scenario: Sign in with valid credentials returns a JWT token
    Given path '/api/v1/authentication/sign-up'
    And request { username: '#(randomUsername)', password: 'Weride2026!' }
    When method post
    Then status 201

    Given path '/api/v1/authentication/sign-in'
    And request { username: '#(randomUsername)', password: 'Weride2026!' }
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.id == '#number'

  Scenario: Sign in with a wrong password is rejected
    Given path '/api/v1/authentication/sign-up'
    And request { username: '#(randomUsername)', password: 'Weride2026!' }
    When method post
    Then status 201

    Given path '/api/v1/authentication/sign-in'
    And request { username: '#(randomUsername)', password: 'WrongPassword!' }
    When method post
    Then status 401