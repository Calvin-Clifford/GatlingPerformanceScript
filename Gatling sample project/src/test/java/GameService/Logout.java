package GameService;

import io.gatling.javaapi.core.ChainBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class Logout {

    /**
     * Generates the logout request chain.
     * @param gmId            The Game ID
     * @param gmName          The Game Name
     * @param gameServiceUrl  The Game Service URL for the logout request
     * @return ChainBuilder   The chain to execute the logout request
     */
    public static ChainBuilder LogoutRequest(String gmId, String gmName, String gameServiceUrl) {
        // Step 1: Set up session data
        return exec(session -> {
            // Retrieve session values for the required headers
            String timestamp = String.valueOf(System.currentTimeMillis());
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
            String origin = session.getString("domain");

            // Step 2: Concatenate values and generate the initial hash
            String initialHashInput = timestamp + userAgent + origin;
            String initialHash = DigestUtils.sha256Hex(initialHashInput);

            // Step 3: Add secret key and timestamp for the final hash
            String secretKey = "SecretKey";
            String secureString = secretKey + "timeStamp";
            String finalHashInput = initialHash + secureString;
            String finalHash = DigestUtils.sha256Hex(finalHashInput);

            // Step 4: Construct the signature header using the timestamp and hash
            String signature = "t=" + timestamp + ",v1=" + finalHash;

            // Step 5: Store the generated signature in the session for later use
            return session.set("signature", signature);
        })

                // Step 6: Execute the logout HTTP request
                .exec(http("Logout - " + gmName)
                                .post(gameServiceUrl)  // POST request to the game service URL
                                .header("accept", "*/*")  // Accept all content types
                                .header("accept-encoding", "gzip, deflate, br, zstd")  // Acceptable encodings
                                .header("accept-language", "en-US,en;q=0.9")  // Set language preference
                                .header("content-type", "application/json")  // Set content type to JSON
                                .header("access-control-request-headers", "content-type,signature")  // Required headers for CORS
                                .header("access-control-request-method", "POST")  // Specify the method for CORS
                                .header("origin", "#{domain}")  // Dynamic placeholder for the origin value from session
                                .header("priority", "u=1, i")  // Set priority for the request
                                .header("referer", "#{domain}/")  // Dynamic referer URL from session
                                .header("sec-fetch-dest", "empty")  // Fetch destination type
                                .header("sec-fetch-mode", "cors")  // CORS mode for the request
                                .header("sec-fetch-site", "cross-site")  // Indicating cross-site interaction
                                .header("signature", session -> session.getString("signature"))  // Use signature from session
                                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")  // User-agent header
                                .body(StringBody(
                                        "{" +
                                                "\"user\":\"#{playerName}\"," +  // Changed from "playerName" to "user"
                                                "\"gameIdentifier\":\"" + gmId + "\"," +  // Changed from "gameId" to "gameIdentifier"
                                                "\"sessionToken\":\"#{gamesessionKey}\"," + // Changed from "sessionKey" to "sessionToken"
                                                "\"operation\":\"gamelogout\"" +          // Changed from "action" to "operation"
                                                "}"
                                ))
                                .check(status().is(200))  // Check for HTTP 200 response
                                .check(jsonPath("$.errorCode").is("100")) // Verify that the error code is 100 (successful logout)
                        // Uncomment below lines if additional checks are required
                        // .check(responseTimeInMillis().saveAs("responseTime"))  // Save response time to session
                        // .check(status().saveAs("statusCode"))  // Save HTTP status code to session
                )

                // Exit the chain if any request fails
                .exitHereIfFailed();
    }
}
