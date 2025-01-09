# Gatling Performance Testing Bundle Guide

This project provides a comprehensive setup for performance testing using **Gatling**, designed to handle multiple server instances, dynamic CSV splitting, and user scenario generation. It is based on the [Gatling Maven Plugin demo in Java](https://github.com/gatling/gatling-maven-plugin-demo-java).

## Project Setup

### Prerequisites

Before you run the tests, ensure you have the following prerequisites installed:

1. **Java 11+** installed on your system.
2. **Maven 3+** installed.
3. **Gatling** configured via Maven.

### Clone the repository

To get started, clone the repository to your local system:

```bash
git clone <your-repository-url>
cd <your-repository-name>
Run Performance Tests Locally
This project allows you to run performance tests across multiple server instances. You can specify the total number of instances and which instance you are currently running.

Running the Test with Multiple Server Instances
You can configure the number of server instances and specify the current server instance by passing system properties when running the tests.

On Linux / MacOS:
bash
Copy code
./mvnw gatling:test "-DNumberofinstance.identifier=<total_instances>" "-Dserver.identifier=<server_instance>"
On Windows:
bash
Copy code
mvnw.cmd gatling:test "-DNumberofinstance.identifier=<total_instances>" "-Dserver.identifier=<server_instance>"
Command Breakdown:

-DNumberofinstance.identifier=<total_instances>: Defines the total number of server instances. Replace <total_instances> with the total number of instances (e.g., 4).
-Dserver.identifier=<server_instance>: Specifies the server instance to run on (e.g., server1, server2, etc.).
Example:

bash
Copy code
./mvnw gatling:test "-DNumberofinstance.identifier=4" "-Dserver.identifier=server1"
This will execute the test for server1 in a setup with 4 instances.

Changing Server Instance
To run the test on a different server instance, update the server.identifier property:

-Dserver.identifier=server2 for the second server instance.
-Dserver.identifier=server3 for the third server instance.
Gatling Recorder
To record your actions and generate performance test scenarios, you can use the Gatling Recorder.

On Linux / MacOS:
bash
Copy code
./mvnw gatling:recorder
On Windows:
bash
Copy code
mvnw.cmd gatling:recorder
CSV User Generation and Splitting
In this project, we dynamically generate CSV files for user simulation based on the configured initial_username and final_username.

Configuration:
properties
Copy code
# CSV splitting
No_of_Scenario=5
initial_username=testuser001
final_username=testuser100
The above configuration will generate a CSV with 5 scenarios, and each user will be named between testuser001 and testuser100. The scenario file will be split accordingly based on the number of scenarios.

Variables to Customize for Your Test
Define the key variables for the test scenario, such as the number of concurrent users, ramp-up duration, and the total duration:

properties
Copy code
# Variables
Spin_Count=15
Pause_Time=2
Ramp_Duration=30
Total_Concurrent_Users=50

# User Counts
A_UserCount=200
This setup allows for flexibility in adjusting user load, pauses, and ramp-up durations.

Example Game Configuration:
Below is an example of the game service URL configuration:

properties
Copy code
# A - Game Service
A_GAME_SERVICE_URL=https://example.com/gameservice/game.do
A_CsvName=GameScenario.csv
A_Gameid=gameid123
A_GameName=Example Game
You can configure the game URLs and other parameters for your specific use case.

Running the Tests
For running tests on a single instance:

Simply run:

bash
Copy code
./mvnw gatling:test
This will use the default configuration without any additional server-specific parameters.

For running tests with multiple server instances:

As previously mentioned, use:

bash
Copy code
./mvnw gatling:test "-DNumberofinstance.identifier=<total_instances>" "-Dserver.identifier=<server_instance>"
This will distribute the load across the specified server instance.

Further Resources
Gatling Scripting Introduction
Gatling Maven Plugin Documentation
Gatling Official Documentation
