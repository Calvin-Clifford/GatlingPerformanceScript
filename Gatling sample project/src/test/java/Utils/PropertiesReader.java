package Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesReader {
    // Properties object to hold configuration values
    private static Properties properties;

    // Path to the properties file. The file location is expected to be inside 'src/test/resources'.
    private static final String propertiesFilePath = System.getProperty("user.dir") + "/src/test/resources/configQA.properties";

    // Static block to initialize the Properties object with values from the properties file
    static {
        properties = new Properties();
        try {
            // Load the properties from the file
            InputStream inputStream = new FileInputStream(propertiesFilePath);
            properties.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            // Print any exception if loading the properties file fails
            e.printStackTrace();
        }
    }

    // *** URL Endpoints ***

    /**
     * Retrieves the game launch endpoint from the properties file.
     * @return The game launch endpoint URL.
     */
    public static String get_GAME_LAUNCH_EP() {
        return properties.getProperty("gamelaunch_endpoint");
    }

    /**
     * Retrieves the base URL for the operator from the properties file.
     * @return The operator's base URL.
     */
    public static String get_OP_BASE_URL() {
        return properties.getProperty("operator_base_Url");
    }

    /**
     * Retrieves the sign-up endpoint from the properties file.
     * @return The sign-up endpoint URL.
     */
    public static String get_OP_SIGNUP_EP() {
        return properties.getProperty("sign_up_endpoint");
    }

    // *** Simulation Configuration ***

    /**
     * Retrieves the total duration of the simulation from the properties file.
     * @return The total duration of the simulation in seconds.
     */
    public static String get_TOTAL_DURATION() {
        return properties.getProperty("Total_Duration");
    }

    /**
     * Retrieves the pause time between actions in the simulation from the properties file.
     * @return The pause time in seconds.
     */
    public static String get_PAUSE_TIME() {
        return properties.getProperty("Pause_Time");
    }

    /**
     * Retrieves the ramp duration from the properties file.
     * @return The ramp duration in seconds.
     */
    public static String get_RAMP_DURATION() {
        return properties.getProperty("Ramp_Duration");
    }

    /**
     * Retrieves the desired total number of concurrent users for the simulation.
     * @return The number of concurrent users.
     */
    public static String get_DESIRED_TOTAL_USERS() {
        return properties.getProperty("Total_Concurrent_Users");
    }

    /**
     * Retrieves the bonus code used in the simulation.
     * @return The bonus code.
     */
    public static String get_Bonus_Code() {
        return properties.getProperty("bonus_code");
    }

    /**
     * Retrieves the total number of bets per minute for the simulation.
     * @return The total bets per minute.
     */
    public static String get_TOTAL_BETS_PER_MINUTE() {
        return properties.getProperty("total_bets_per_min");
    }

    // *** User Configuration ***

    /**
     * Retrieves the number of users for scenario A.
     * @return The number of users for scenario A.
     */
    public static String get_A_USER_COUNT() {
        return properties.getProperty("A_UserCount");
    }

    // *** CSV Configuration ***

    /**
     * Retrieves the number of scenarios to be run in the simulation.
     * @return The number of scenarios.
     */
    public static String get_No_of_Scenario() {
        return properties.getProperty("No_of_Scenario");
    }

    /**
     * Retrieves the initial username for user generation in the simulation.
     * @return The initial username.
     */
    public static String get_initial_username() {
        return properties.getProperty("initial_username");
    }

    /**
     * Retrieves the final username for user generation in the simulation.
     * @return The final username.
     */
    public static String get_final_username() {
        return properties.getProperty("final_username");
    }

    // *** Game-Specific Configuration (Generic Slot Game) ***

    /**
     * Retrieves the game ID for the slot game.
     * @return The game ID for the slot game.
     */
    public static String getAGameid() {
        return properties.getProperty("A_Gameid");
    }

    /**
     * Retrieves the game name for the slot game.
     * @return The game name for the slot game.
     */
    public static String getAGameName() {
        return properties.getProperty("A_GameName");
    }

    /**
     * Retrieves the game service URL for the slot game.
     * @return The game service URL for the slot game.
     */
    public static String get_A_GAME_SERVICE_URL() {
        return properties.getProperty("A_GAME_SERVICE_URL");
    }
}
