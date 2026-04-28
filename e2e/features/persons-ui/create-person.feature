@ui @db
Feature: Create person via UI

  Scenario: Authenticated user creates a person and the row lands in DB
    Given I am logged in as "test@example.com"
    When I open the persons page
    And I click the add person button
    And I fill in the person form with first name "E2ETest" and last name "CreateUser" and email "e2e-test-create@example.com"
    And I submit the person form
    Then the persons list shows "E2ETest"
    And the database has a person with email "e2e-test-create@example.com"
