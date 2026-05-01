@auth @ui
Feature: User registration and profile management


  Scenario: Successful registration and login
    Given I visit the registration page
    When I fill in valid registration details
    And I submit the registration form
    Then I see the registration success message
    When I sign in via the login form with the registered email
    Then I am on the persons page and can see my display name in the nav

  Scenario: Protected routes require login
    Given I am not authenticated
    When I navigate to the persons page directly
    Then I am redirected to the login page

  Scenario: Previously public endpoint now requires authentication
    Given I am not authenticated
    When I request the persons API endpoint without authentication
    Then the response status is 401

  Scenario: Edit display name
    Given I have a registered and active test account
    And I navigate to my profile page
    When I edit my display name to "E2E Updated Name"
    Then the profile page shows the display name "E2E Updated Name"

  Scenario: Upload and remove avatar
    Given I have a registered and active test account
    And I navigate to my profile page
    When I upload a valid PNG avatar
    Then the avatar image is visible on the profile page
    When I remove the avatar
    Then the placeholder icon is shown on the profile page

  Scenario: Account deletion
    Given I have a registered and active test account
    And I navigate to my profile page
    When I delete my account from the profile page
    Then I am redirected and no longer authenticated
    And the deleted account cannot be accessed with the old token
