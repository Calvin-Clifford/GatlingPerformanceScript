package GameService;

import io.gatling.javaapi.core.ChainBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class Init {

    /**
     * Starts the service initialization request.
     *
     * @param gameLabel      The game identifier for logging requests.
     * @param serviceUrl     The endpoint URL of the service.
     * @return ChainBuilder  The sequence of HTTP requests to execute.
     */
    public static ChainBuilder InitRequest(String gameLabel, String serviceUrl) {
        return exec(session -> {
            // Step 1: Retrieve session values and header details.
            String requestAction = "startgame";
            String sessionToken = session.getString("authToken");
            String browserInfo = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
            String referrerDomain = session.getString("domainUrl");

            // Step 2: Generate the current timestamp for the request.
            String requestTimestamp = String.valueOf(System.currentTimeMillis());

            // Step 3: Combine sessionToken, requestAction, timestamp, browserInfo, and referrerDomain to create an input string for hashing.
            String preHashString = sessionToken + requestAction + requestTimestamp + browserInfo + referrerDomain;
            String hashedString = DigestUtils.sha256Hex(preHashString); // Create SHA-256 hash.

            // Step 4: Combine a secret key with the other parameters.
            String secret = "SuperSecretKey"; // Your secret key for hashing.
            String secureHashInput = secret + "token" + "action" + "timestamp";

            // Step 5: Combine the initial hash and secure string to generate the final hash.
            String finalHashInput = hashedString + secureHashInput;
            String finalHash = DigestUtils.sha256Hex(finalHashInput); // Final SHA-256 hash.

            // Step 6: Create the signature header with timestamp and final hash.
            String requestSignature = "t=" + requestTimestamp + ",v1=" + finalHash;

            // Step 7: Store the signature in the session for use in future requests.
            return session.set("authSignature", requestSignature);
        })

                // Step 8: Execute HTTP OPTIONS request to check for CORS preflight.
                .exec(http("Options Request - " + gameLabel)
                        .options(serviceUrl)
                        .header("accept", "*/*")
                        .header("accept-encoding", "gzip, deflate, br, zstd")
                        .header("accept-language", "en-US,en;q=0.9")
                        .header("access-control-request-headers", "content-type,authSignature")
                        .header("access-control-request-method", "POST")
                        .header("origin", "#{domainUrl}")
                        .header("priority", "u=1, i")
                        .header("referer", "#{domainUrl}/")
                        .header("sec-fetch-dest", "empty")
                        .header("sec-fetch-mode", "cors")
                        .header("sec-fetch-site", "cross-site")
                        .header("authSignature", session -> session.getString("authSignature"))
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                        .check(status().is(202))
                )

                // Step 9: Execute HTTP POST request to initialize the game service.
                .exec(http("Init Request - " + gameLabel)
                        .post(serviceUrl)
                        .header("accept", "*/*")
                        .header("accept-encoding", "gzip, deflate, br, zstd")
                        .header("accept-language", "en-US,en;q=0.9")
                        .header("content-type", "application/json")
                        .header("access-control-request-headers", "content-type,authSignature")
                        .header("access-control-request-method", "POST")
                        .header("origin", "#{domainUrl}")
                        .header("priority", "u=1, i")
                        .header("referer", "#{domainUrl}/")
                        .header("sec-fetch-dest", "empty")
                        .header("sec-fetch-mode", "cors")
                        .header("sec-fetch-site", "cross-site")
                        .header("authSignature", session -> session.getString("authSignature"))
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")

                        // Step 10: Construct the request body in JSON format with dynamic placeholders.
                        .body(StringBody(
                                "{" +
                                        "\"authToken\":\"#{gameAuthToken}\"," +
                                        "\"action\":\"startgame\"}"))

                        .check(status().is(200))
                        .check(jsonPath("$.status").saveAs("gameStatus"))
                )

                // Step 11: Exit the chain if any request fails.
                .exitHereIfFailed();
    }
}
