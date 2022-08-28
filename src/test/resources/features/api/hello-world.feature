
Feature: Hello world

Scenario: Hello world GET api Test
  Given User sends get request to hello world api
  Then hello world api status code is 200
  And hello world api response body contains "Hello World!"