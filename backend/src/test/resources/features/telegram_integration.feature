Feature: Telegram Integration

  Scenario: Generate Telegram verification code
    Given a user "Sascha" is logged in
    When "Sascha" requests a Telegram verification code
    Then a unique verification code is generated
    And the code is associated with "Sascha"

  Scenario: Link Telegram account with valid code
    Given a user "Sascha" has verification code "ABC-123-XYZ"
    And Telegram account sends link request with code "ABC-123-XYZ" and chat ID 123456
    When the link request is processed
    Then "Sascha" is linked to Telegram chat 123456
    And the verification code is cleared
