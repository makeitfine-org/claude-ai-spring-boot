@auth @ui
Feature: Login

  Scenario: Login page shows email and password fields and a Sign in button
    Given I am on the login page
    Then I should see the email field
    And I should see the password field
    And I should see the sign in button

  Scenario: Successful login with valid credentials redirects to persons page
    Given I am logged in as "test@example.com"
    Then I should be redirected to the persons page

  Scenario: Logout returns the user to the login page
    Given I am logged in as "test@example.com"
    Then I should be redirected to the persons page
    When I click the logout button
    Then I am redirected to the login page
