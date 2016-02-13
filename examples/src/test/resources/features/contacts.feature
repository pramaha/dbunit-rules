Feature: Contacts test
  As a user of contacts repository
  I want to crud contacts
  So that I can expose contacts service


  Scenario Outline: search contacts
    Given we have contacts in the database
    When we search contacts by name "<name>"
    Then we should find <result> contacts

  Examples: examples1
  | name     | result |
  | delta    | 1      |
  | sp       | 2      |
  | querydsl | 1      |
  | abcd     | 0      |


  Scenario: delete a contact

    Given we have contacts in the database
    When we delete contact by id 1
    Then we should not find contacts 1 in database