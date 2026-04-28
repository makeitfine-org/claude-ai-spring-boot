@ui
Feature: List, search and sort persons

  Scenario: Persons page shows list of persons
    Given I am logged in as "test@example.com"
    When I open the persons page
    Then the persons list shows at least one row

  Scenario: Search filters the list
    Given I am logged in as "test@example.com"
    When I open the persons page
    And I search for "Smith"
    Then the persons list shows "Smith"
