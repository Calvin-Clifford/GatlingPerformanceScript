package Runner;

import Utils.PropertiesReader;
import Utils.UserCsvSplitter;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static CsrfFetcher.FetchCsrf.fetchCsrfrequest;
import static Registration.Registration.registrationRequest;
import static GameLaunch.GameLauncher.gameLaunchRequest;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SimulationRunner extends Simulation {

    // *** Configuration Parameters ***
    // URLs for various endpoints
    String GAME_LAUNCH_EP = PropertiesReader.get_GAME_LAUNCH_EP();  // Game launch endpoint
    String OP_BASE_URL = PropertiesReader.get_OP_BASE_URL();  // Base URL for operations
    String OP_SIGNUP_EP = PropertiesReader.get_OP_SIGNUP_EP();  // Sign-up endpoint

    // Simulation parameters
    long TOTAL_DURATION = Long.parseLong(PropertiesReader.get_TOTAL_DURATION());  // Total duration of simulation
    long PAUSE_TIME = Long.parseLong(PropertiesReader.get_PAUSE_TIME());  // Pause time between actions
    int RAMP_DURATION = Integer.parseInt(PropertiesReader.get_RAMP_DURATION());  // Ramp-up duration for user injection
    AtomicInteger readyUsers = new AtomicInteger(0);  // Tracks the number of ready users
    int TOTAL_USER_COUNT = 0;  // Total number of users
    int TOTAL_BETS_PER_MINUTE = Integer.parseInt(PropertiesReader.get_TOTAL_BETS_PER_MINUTE());  // Total bets per minute

    // User configurations
    int A_USER_COUNT = Integer.parseInt(PropertiesReader.get_A_USER_COUNT());  // User count for scenario A

    // Nordic Slots game configurations
    String A_GAME_SERVICE_URL = PropertiesReader.get_A_GAME_SERVICE_URL();  // URL for the game service
    String A_gmid = PropertiesReader.getAGameid();  // Game ID
    String A_gmname = PropertiesReader.getAGameName();  // Game name

    // User range and scenario configurations
    String startUser = PropertiesReader.get_initial_username();  // Initial username for the scenario
    String endUser = PropertiesReader.get_final_username();  // Final username for the scenario
    int numScenarios = Integer.parseInt(PropertiesReader.get_No_of_Scenario());  // Number of scenarios
    String Bonuscode = PropertiesReader.get_Bonus_Code();  // Bonus code

    // Instance and server information
    int totalInstances;  // Total instances for distribution of users
    String serverId;  // Server identifier
    long BET_PAUSE_TIME;  // Pause time between bets

    // Initialize user chunks and user list
    {
        // Get instance and server ID from system properties
        totalInstances = Integer.parseInt(System.getProperty("Numberofinstance.identifier", "1"));
        serverId = System.getProperty("server.identifier", "server1");

        // List of user counts for different scenarios
        List<Integer> userCounts = List.of(A_USER_COUNT);

        // Divide user counts equally across instances
        List<Integer> updatedUserCounts = new ArrayList<>();
        int totalAssignedUsers = 0;
        for (int userCount : userCounts) {
            int dividedUserCount = userCount / totalInstances;
            updatedUserCounts.add(dividedUserCount);
            totalAssignedUsers += dividedUserCount;
        }

        // Update user count for scenario A
        A_USER_COUNT = updatedUserCounts.get(0);

        // Calculate the bet pause time for the server and users
        int betsPerMinuteForServer = TOTAL_BETS_PER_MINUTE / totalInstances;
        int betsPerMinutePerUser = betsPerMinuteForServer / A_USER_COUNT;
        BET_PAUSE_TIME = (60) / betsPerMinutePerUser;

        // Generate user chunks in memory for distribution across instances
        UserCsvSplitter.generateUserChunks(startUser, endUser, numScenarios, totalInstances);

        // Fetch users for the current instance
        List<Map<String, String>> usersForThisInstance = UserCsvSplitter.getUsersForScenario(serverId, totalInstances);

        // Log the number of users for the current server instance
        System.out.println("Server: " + serverId + ", Number of users: " + usersForThisInstance.size());

        // Print chunk details (number of users per chunk) for debugging purposes
        UserCsvSplitter.printChunkDetails();

        // Get the CSV file from resources
        UserCsvSplitter.getCsvFileFromResources("target/test-classes");

        // Simulate a 5-second delay before starting the simulation
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // *** HTTP Protocol Setup ***
    HttpProtocolBuilder httpProtocol = http
            .acceptHeader("application/json");  // Set the content type for HTTP requests

    // *** Scenario Definition ***
    // Scenario A: User Registration and Game Play
    ScenarioBuilder A = scenario("User Registration and Game Play")
            .exec(fetchCsrfrequest(OP_BASE_URL))  // Fetch CSRF token for the session
            .pause(PAUSE_TIME)  // Pause before the next action
            .exec(registrationRequest(OP_BASE_URL, OP_SIGNUP_EP, Bonuscode, UserCsvSplitter.getCsvFileForScenario(1, serverId), readyUsers, TOTAL_USER_COUNT))  // Registration request
            .pause(PAUSE_TIME)  // Pause before the next action
            .rendezVous(A_USER_COUNT)  // Rendezvous point for A_USER_COUNT users
            .exec(gameLaunchRequest(OP_BASE_URL, GAME_LAUNCH_EP, A_gmid, A_gmname, readyUsers, TOTAL_USER_COUNT))  // Game launch request
            .pause(PAUSE_TIME)  // Pause before the next action
            .exec(GameService.Init.InitRequest(A_gmname, A_GAME_SERVICE_URL))  // Initial request to the game service
            .pause(PAUSE_TIME)  // Pause before the next action
            .exec(session -> session.set("errorCode", "200"))  // Set default error code
            .asLongAs(session -> "3".equals(session.getString("state")) && "200".equals(session.getString("errorCode")))  // Loop until conditions are met
            .on(
                    exec(GameService.FreeSpin.FreeSpinRequest(A_gmid, A_gmname, A_GAME_SERVICE_URL))  // Free spin request
            )
            .during(TOTAL_DURATION).on(
                    exec(GameService.Spin.SpinRequest(A_gmid, A_gmname, A_GAME_SERVICE_URL))  // Spin request
                            .pause(PAUSE_TIME)
                            .exec(session -> session.set("errorCode", "200"))
                            .asLongAs(session -> "3".equals(session.getString("state")) && "200".equals(session.getString("errorCode")))  // Loop for spins
                            .on(
                                    exec(GameService.FreeSpin.FreeSpinRequest(A_gmid, A_gmname, A_GAME_SERVICE_URL))  // Free spin request during spin
                            )
                            .exec(GameService.End.EndRequest(A_gmid, A_gmname, A_GAME_SERVICE_URL))  // End request
                            .pause(BET_PAUSE_TIME)  // Pause before next bet
            )
            .pause(PAUSE_TIME)  // Pause before the next action
            .exec(GameService.Logout.LogoutRequest(A_gmid, A_gmname, A_GAME_SERVICE_URL));  // Logout request

    // *** Simulation Setup ***
    {
        // Set up the simulation with user injection and HTTP protocol
        setUp(
                A.injectOpen(rampUsers(A_USER_COUNT).during(RAMP_DURATION))  // Inject users gradually over the ramp duration
        ).protocols(httpProtocol);
    }

    // *** Cleanup After Simulation ***
    @Override
    public void after() {
        // Clean up CSV files after simulation
        Utils.UserCsvSplitter.deleteAllCsvFiles();
        System.out.println("Simulation is finished!");
    }

}
