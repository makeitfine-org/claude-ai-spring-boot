@auth @ui
Feature: Login

  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I enter email "test@example.com" and password "password"
    And I submit the login form
    Then I should be redirected to the persons page

  Scenario: Login fails with invalid credentials
    Given I am on the login page
    When I enter email "wrong@example.com" and password "wrongpassword"
    And I submit the login form
    Then I should see an error message
