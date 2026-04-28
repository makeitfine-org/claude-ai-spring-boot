@ui @db
Feature: Delete person via UI

  Scenario: Authenticated user deletes a person
    Given I am logged in as "test@example.com"
    And I open the persons page
    And I click the add person button
    And I fill in the person form with first name "E2ETest" and last name "DeleteMe" and email "e2e-test-delete@example.com"
    And I submit the person form
    When I click delete for "E2ETest DeleteMe"
    And I confirm the deletion
    Then the persons list does not show "E2ETest DeleteMe"
    And the database does not have a person with email "e2e-test-delete@example.com"
