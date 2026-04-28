@api
Feature: Person CRUD via REST API

  Scenario: Create, read, update and delete a person
    Given I have a valid auth token
    When I create a person via API with first name "E2ETest" last name "ApiUser" email "e2e-test-api@example.com"
    Then the API response status is 201
    And the API response contains first name "E2ETest"
    When I retrieve the person by id
    Then the API response status is 200
    When I update the person last name to "ApiUpdated" via API
    Then the API response status is 200
    When I delete the person via API
    Then the API response status is 204
