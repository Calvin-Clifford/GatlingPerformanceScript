package GameService;

import io.gatling.javaapi.core.ChainBuilder;
import org.apache.commons.codec.digest.DigestUtils;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class FreeSpin {

    /**
     * This method simulates the FreeSpin action for a game service.
     * It handles the generation of token, OPTIONS request, and POST request.
     *
     * @param gmId - Game ID
     * @param gmName - Game Name
     * @param gameServiceUrl - Game Service URL
     * @return ChainBuilder - The chain of HTTP requests for the FreeSpin action
     */
    public static ChainBuilder FreeSpinRequest(String gmId, String gmName, String gameServiceUrl) {
        return exec(session -> {
            // Step 1: Retrieve necessary session and header values
            String timestamp = String.valueOf(System.currentTimeMillis());  // Current timestamp
            String sessionToken = session.get("sessionToken");  // Retrieve session token from session
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";  // User-Agent string
            String origin = session.getString("origin");  // Retrieve origin from session

            // Step 2: Generate initial hash input using timestamp, session token, user-agent, and origin
            String initialHashInput = timestamp + sessionToken + userAgent + origin;
            String initialHash = DigestUtils.sha256Hex(initialHashInput);  // SHA-256 hash of the initial input

            // Step 3: Concatenate secret key with other fields for the final hash
            String secretKey = "SecretKey";  // Secret key (assumed to be static)
            String secureString = secretKey + "timestamp" + "sessionToken";  // Prepare secure string for final hash
            String finalHashInput = initialHash + secureString;  // Final input for hashing
            String finalHash = DigestUtils.sha256Hex(finalHashInput);  // SHA-256 hash of the final input

            // Step 4: Construct the token header
            String token = "t=" + timestamp + ",v1=" + finalHash;

            // Step 5: Save the generated token in the session for future use
            return session.set("token", token);  // Store token in the session
        })

                // Step 6: Send an OPTIONS request to check the CORS permissions and headers
                .exec(http("Options - " + gmName)
                        .options(gameServiceUrl)  // OPTIONS request
                        .header("accept", "*/*")  // Accept all content types
                        .header("accept-encoding", "gzip, deflate, br, zstd")  // Acceptable encodings
                        .header("accept-language", "en-US,en;q=0.9")  // Set accept language
                        .header("access-control-request-headers", "content-type,token")  // Request headers
                        .header("access-control-request-method", "POST")  // Request method
                        .header("origin", "#{origin}")  // Dynamic placeholder for origin
                        .header("priority", "u=1, i")  // Set priority
                        .header("referer", "#{origin}/")  // Dynamic referer URL
                        .header("sec-fetch-dest", "empty")  // Fetch destination
                        .header("sec-fetch-mode", "cors")  // CORS mode
                        .header("sec-fetch-site", "cross-site")  // Site interaction mode
                        .header("token", session -> session.getString("token"))  // Include token from session
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")  // User-Agent header
                        .check(status().is(202))  // Expect HTTP 202 response for the OPTIONS request
                )

                // Step 7: Send a POST request for FreeSpin action
                .exec(http("FreeSpin - " + gmName)
                        .post(gameServiceUrl)  // POST request to game service URL
                        .header("accept", "*/*")  // Accept all content types
                        .header("accept-encoding", "gzip, deflate, br, zstd")  // Acceptable encodings
                        .header("accept-language", "en-US,en;q=0.9")  // Set accept language
                        .header("access-control-request-headers", "content-type,token")  // Request headers
                        .header("access-control-request-method", "POST")  // Request method
                        .header("content-type", "application/json")  // Set content type to JSON
                        .header("origin", "#{origin}")  // Dynamic origin
                        .header("priority", "u=1, i")  // Set priority
                        .header("referer", "#{origin}/")  // Dynamic referer URL
                        .header("sec-fetch-dest", "empty")  // Fetch destination
                        .header("sec-fetch-mode", "cors")  // CORS mode
                        .header("sec-fetch-site", "cross-site")  // Site interaction mode
                        .header("token", session -> session.getString("token"))  // Token from session
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")  // User-Agent header
                        .body(StringBody(
                                "{" +
                                        "\"username\":\"#{username}\"," +  // Dynamic player name from session (renamed playerName to username)
                                        "\"gameReference\":\"" + gmId + "\"," +  // Dynamic game ID passed to method (renamed gameId to gameReference)
                                        "\"sessionToken\":\"#{sessionToken}\"," + // Dynamic session token from session
                                        "\"action\":\"freeSpinAction\"" +  // Static value for action (renamed freespinaction to freeSpinAction)
                                        "}"))
                        .check(status().is(200))  // Check for a successful response
                        .check(jsonPath("$.state").saveAs("state"))  // Extract state and save to session
                        .check(jsonPath("$.errorCode").saveAs("errorCode"))  // Extract error code and save to session
                );
    }
}
