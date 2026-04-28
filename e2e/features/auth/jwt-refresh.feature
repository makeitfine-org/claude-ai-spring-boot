@auth @api
Feature: JWT refresh

  Scenario: Refresh token returns new access token
    Given I have a valid JWT token
    When I call the refresh endpoint with my refresh token
    Then I should receive a new access token
