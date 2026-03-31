Feature: Task Management

  Scenario: User completes a task
    Given a user "Sascha" exists in household
    And "Sascha" has a pending task "Kochen" for today
    When "Sascha" completes the task "Kochen"
    Then the task "Kochen" is marked as completed
    And the points for "Sascha" increase by 1

  Scenario: User skips a task
    Given a user "Sascha" and "Alexandra" exist in household
    And "Sascha" has a pending task "Kochen" for today
    When "Sascha" skips the task "Kochen"
    Then the task "Kochen" is marked as skipped
    And "Alexandra" has a new task "Kochen" for tomorrow
    And the points for "Sascha" remain unchanged

  Scenario: User moves a task to next day
    Given a user "Sascha" has a pending task "Saugen" for today
    When "Sascha" moves the task "Saugen" to tomorrow
    Then the task "Saugen" is marked as moved
    And "Sascha" has the task "Saugen" for tomorrow

  Scenario: Get today's tasks
    Given users "Sascha" and "Alexandra" exist in household
    And "Sascha" has tasks "Kochen", "Abwasch" for today
    And "Alexandra" has tasks "Müll rausbringen" for today
    When I request today's tasks
    Then I receive 3 tasks total
