@db @api
Feature: Database state integrity

  Scenario: Creating a person via API lands in database
    Given I have a valid auth token
    When I create a person via API with first name "E2ETest" last name "DbCheck" email "e2e-test-dbcheck@example.com"
    Then the API response status is 201
    And the database has a person with email "e2e-test-dbcheck@example.com"
