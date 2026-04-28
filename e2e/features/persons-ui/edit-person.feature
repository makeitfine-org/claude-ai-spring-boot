@ui @db
Feature: Edit person via UI

  Scenario: Authenticated user edits a person
    Given I am logged in as "test@example.com"
    And I open the persons page
    And I click the add person button
    And I fill in the person form with first name "E2ETest" and last name "EditBefore" and email "e2e-test-edit@example.com"
    And I submit the person form
    When I click edit for "E2ETest EditBefore"
    And I update the last name to "EditAfter"
    And I submit the person form
    Then the persons list shows "EditAfter"
    And the database person with email "e2e-test-edit@example.com" has last name "EditAfter"
