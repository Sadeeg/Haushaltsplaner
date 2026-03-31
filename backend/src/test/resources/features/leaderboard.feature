Feature: Leaderboard

  Scenario: View leaderboard
    Given users "Sascha" and "Alexandra" exist in household
    And "Sascha" has completed 5 tasks with 5 points
    And "Alexandra" has completed 3 tasks with 3 points
    When I view the leaderboard
    Then "Sascha" is in first place with 5 points
    And "Alexandra" is in second place with 3 points
