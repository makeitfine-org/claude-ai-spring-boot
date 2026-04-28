@api
Feature: Person list pagination

  Scenario: List persons returns paginated results
    Given I have a valid auth token
    When I list persons via API
    Then the API response status is 200
    And the API response contains at least 1 persons
