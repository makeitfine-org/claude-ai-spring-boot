@auth @ui
Feature: Login

  Scenario: Login prompt page shows Sign in button
    Given I am on the login page
    Then I should see the sign in button

  Scenario: Successful login with valid credentials redirects to persons page
    Given I am logged in as "test@example.com"
    Then I should be redirected to the persons page
