package GameService;

import io.gatling.javaapi.core.ChainBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class Spin {

    /**
     * Sends a spin action request for a game.
     * @param gmId The game ID.
     * @param gmName The name of the game.
     * @param gameServiceUrl The URL of the game service API.
     * @return A ChainBuilder representing the steps for the spin request.
     */
    public static ChainBuilder SpinRequest(String gmId, String gmName, String gameServiceUrl) {
        return exec(session -> {
            // Step 1: Retrieve session variables and define static values
            String action = "spinaction"; // Action for spinning
            String timestamp = String.valueOf(System.currentTimeMillis()); // Get current timestamp
            int betAmtPerLine = 1; // Bet amount per line (static value for this example)
            int lines = 20; // Number of lines (static value for this example)
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"; // User-Agent string
            String origin = session.getString("domain"); // Retrieve the domain from the session

            // Step 2: Concatenate values in the specified order to create an initial hash
            String initialHashInput = lines + action + timestamp + betAmtPerLine + userAgent + origin;
            String initialHash = DigestUtils.sha256Hex(initialHashInput); // Create SHA-256 hash of the concatenated string

            // Step 3: Concatenate the secret key with other fields to generate the final hash
            String secretKey = "SecretKey"; // Secret key for signing
            String secureString = secretKey + "lines" + "action" + "timeStamp" + "betAmtPerLine"; // Concatenate parameters
            String finalHashInput = initialHash + secureString; // Concatenate initial hash and secure string
            String finalHash = DigestUtils.sha256Hex(finalHashInput); // Final SHA-256 hash

            // Step 4: Construct the signature header
            String signature = "t=" + timestamp + ",v1=" + finalHash; // Build the signature string

            // Step 5: Store the generated signature in the session
            return session.set("signature", signature);
        })
                // Step 6: Send the OPTIONS request to check pre-flight CORS options
                .exec(http("Options - " + gmName)
                        .options(gameServiceUrl)
                        .header("accept", "*/*")
                        .header("accept-encoding", "gzip, deflate, br, zstd")
                        .header("accept-language", "en-US,en;q=0.9")
                        .header("access-control-request-headers", "content-type,signature") // Requesting specific headers
                        .header("access-control-request-method", "POST") // Requesting POST method
                        .header("origin", "#{domain}") // Dynamic placeholder for origin
                        .header("priority", "u=1, i")
                        .header("referer", "#{domain}/") // Dynamic placeholder for referer
                        .header("sec-fetch-dest", "empty")
                        .header("sec-fetch-mode", "cors")
                        .header("sec-fetch-site", "cross-site")
                        .header("Signature", session -> session.getString("signature")) // Use signature from session
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36") // User-agent header
                        .check(status().is(202)) // Check for status 202 (Accepted)
                )
                // Step 7: Send the POST request to trigger the spin action
                .exec(http("Spin - " + gmName)
                        .post(gameServiceUrl)
                        .header("accept", "*/*")
                        .header("accept-encoding", "gzip, deflate, br, zstd")
                        .header("accept-language", "en-US,en;q=0.9")
                        .header("content-type", "application/json")
                        .header("origin", "#{domain}") // Dynamic placeholder for origin
                        .header("priority", "u=1, i")
                        .header("referer", "#{domain}/") // Dynamic placeholder for referer
                        .header("sec-fetch-dest", "empty")
                        .header("sec-fetch-mode", "cors")
                        .header("sec-fetch-site", "cross-site")
                        .header("Signature", session -> session.getString("signature")) // Use signature from session
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36") // User-agent header
                        .body(StringBody(
                                "{" +
                                        "\"username\":\"#{playerName}\"," +  // Changed key from playerName to username
                                        "\"gameId\":\"" + gmId + "\"," +      // Game ID passed dynamically
                                        "\"sessionId\":\"#{gamesessionKey}\"," + // Changed key from sessionKey to sessionId
                                        "\"betAmount\":1," +               // Changed key from betAmtPerLine to betAmount
                                        "\"linesCount\":20," +                      // Changed key from lines to linesCount
                                        "\"actionType\":\"spinaction\"," +         // Changed key from action to actionType
                                        "\"spinMode\":0" +                     // Changed key from spinType to spinMode
                                        "}"
                        ))
                        .check(status().is(200)) // Check for success (HTTP 200)
                        .check(jsonPath("$.state").saveAs("state")) // Check and save the state from the response
                )
                // Step 8: Exit the chain if any request fails
                .exitHereIfFailed();
    }
}
