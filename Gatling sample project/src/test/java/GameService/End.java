package GameService;

import io.gatling.javaapi.core.ChainBuilder;
import org.apache.commons.codec.digest.DigestUtils;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class End {

    /**
     * This method defines the end request in the game service.
     * It calculates the token using session values, constructs the headers,
     * and sends the POST request to the provided game service URL.
     *
     * @param gameId - Game ID for the specific game
     * @param gameName - Name of the game
     * @param serviceUrl - URL of the game service where the request will be sent
     * @return ChainBuilder - Gatling ChainBuilder for chaining requests in the scenario
     */
    public static ChainBuilder EndRequest(String gameId, String gameName, String serviceUrl) {
        return exec(session -> {
            // Step 1: Retrieve session values and header values
            String actionType = "endGame"; // Action for this request (fixed as 'endGame')
            String timestamp = String.valueOf(System.currentTimeMillis()); // Get the current timestamp
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"; // Fixed user agent string
            String originDomain = session.getString("domain"); // Retrieve 'domain' from session for dynamic origin header

            // Step 2: Concatenate values to create the initial hash input
            String hashInput = timestamp + actionType + userAgent + originDomain;
            String intermediateHash = DigestUtils.sha256Hex(hashInput); // Generate SHA-256 hash of the concatenated values

            // Step 3: Add secret key and other parameters to generate the final hash
            String secretKey = "SecretKey123"; // Secret key used for generating the final hash
            String secureString = secretKey + "timestamp" + "actionType"; // Concatenate secret key with timestamp and action for final input
            String finalInput = intermediateHash + secureString; // Combine intermediate hash with secure string
            String finalHash = DigestUtils.sha256Hex(finalInput); // Generate final SHA-256 hash

            // Step 4: Construct the token header using the timestamp and final hash
            String token = "t=" + timestamp + ",v1=" + finalHash; // Token format: timestamp and hash

            // Step 5: Store the token in the session for use in the HTTP request
            return session.set("token", token); // Set the generated token in the session
        })

                // Step 6: Perform the HTTP request to the game service URL
                .exec(http("End - " + gameName) // Name the request dynamically based on the game name
                        .post(serviceUrl) // POST request to the specified game service URL
                        .header("accept", "*/*")  // Accept any content type
                        .header("accept-encoding", "gzip, deflate, br, zstd")  // Acceptable encodings for the response
                        .header("accept-language", "en-US,en;q=0.9")  // Language preference for the response
                        .header("content-type", "application/json") // Specify content type as JSON
                        .header("access-control-request-headers", "content-type,token")  // Specify allowed headers for CORS
                        .header("access-control-request-method", "POST")  // Specify the allowed method for CORS
                        .header("origin", "#{domain}") // Dynamic origin header using session value
                        .header("priority", "u=1, i")  // Priority header (adjustable as needed)
                        .header("referer", "#{domain}/") // Dynamic referer header using session value
                        .header("sec-fetch-dest", "empty")  // Specify fetch destination
                        .header("sec-fetch-mode", "cors")  // Set fetch mode to CORS
                        .header("sec-fetch-site", "cross-site")  // Set fetch site mode to cross-site
                        .header("token", session -> session.getString("token")) // Dynamic token from session
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")  // Fixed user-agent string for the request
                        .body(StringBody( // JSON body for the POST request
                                "{" +
                                        "\"username\":\"#{playerName}\"," +  // Dynamic player name from session
                                        "\"gameReference\":\"" + gameId + "\"," +      // Dynamic game ID (renamed gameId to gameReference)
                                        "\"sessionToken\":\"#{gamesessionKey}\"," + // Dynamic session token (renamed sessionKey to sessionToken)
                                        "\"action\":\"endGame\"" +          // Static action value (renamed action to endGame)
                                        "}"))
                        .check(status().is(200))  // Check if the status code is 200 (OK)
                        .check(jsonPath("$.status").saveAs("status")) // Check if the response contains the 'status' field and save it to the session (renamed state to status)
                )
                .exitHereIfFailed();  // Exit the chain if any check fails
    }
}
