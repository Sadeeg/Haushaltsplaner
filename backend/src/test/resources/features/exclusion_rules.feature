Feature: Exclusion Rules

  Scenario: Create exclusion rule
    Given tasks "Kochen" and "Abwasch" exist in household
    When I create an exclusion rule: "Kochen" and "Abwasch" cannot be assigned to the same person
    Then the exclusion rule is saved

  Scenario: Task assignment respects exclusion rules
    Given an exclusion rule: "Kochen" and "Abwasch" cannot be same person
    And "Sascha" is assigned "Kochen" for today
    When I assign "Abwasch" for today
    Then "Sascha" should not be assigned "Abwasch"
