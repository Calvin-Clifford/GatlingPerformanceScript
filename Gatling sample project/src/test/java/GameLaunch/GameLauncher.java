package GameLaunch;

import Utils.PropertiesReader;
import io.gatling.javaapi.core.ChainBuilder;
import org.json.JSONObject;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class GameLauncher {

    /**
     * This method handles the game launch request.
     * It fetches the game URL and processes the response to extract required information.
     *
     * @param baseurl            The base URL for the game launch request
     * @param gamelaunchurl      The URL for launching the game
     * @param gmId               The game ID
     * @param gmName             The name of the game
     * @param readyUsers         The atomic counter to track ready users
     * @param targetReadyUsers   The target number of ready users
     * @return                   A ChainBuilder representing the scenario for game launch
     */
    public static ChainBuilder gameLaunchRequest(String baseurl, String gamelaunchurl, String gmId, String gmName, AtomicInteger readyUsers, int targetReadyUsers) {
        // Step 1: Send GET request to launch the game and fetch the game URL
        return exec(http("GameLaunch - " + gmName)
                        .get(baseurl + gamelaunchurl)
                        .queryParam("gameId", gmId)  // Pass game ID as query param
                        .queryParam("mode", "real")  // Pass mode as 'real'
                        .queryParam("lobby_url", baseurl)  // Pass the base URL as lobby URL
                        .header("Accept-Encoding", "gzip, deflate, br")  // Accept-Encoding header
                        .header("Accept-Language", "en-US,en;q=0.9")  // Accept-Language header
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.70 Safari/537.36")  // User-Agent header
                        .header("X-Requested-With", "XMLHttpRequest")  // X-Requested-With header
                        .header("Origin", "#{origin}")  // Origin header from the session
                        .header("Referer", "#{origin}")  // Referer header from the session
                        .header("Sec-Fetch-Site", "same-origin")  // Sec-Fetch-Site header
                        .header("Sec-Fetch-Mode", "cors")  // Sec-Fetch-Mode header
                        .header("Sec-Fetch-Dest", "empty")  // Sec-Fetch-Dest header
                        .check(status().is(200))  // Check if the status code is 200 (OK)
                        .check(jsonPath("$.gameUrl").find().saveAs("gameUrl"))  // Extract game URL from the response and save it in the session

                // Step 2: Process the game URL to extract dynamic components
        )
                .exec(session -> {
                    String gameUrl = session.getString("gameUrl");  // Retrieve the game URL from session

                    if (gameUrl == null || !gameUrl.contains("token=")) {
                        // If token is not found in the game URL, log an error and exit
                        System.err.println("Error: Token not found in gameUrl!");
                        exitHereIfFailed();  // Exit if token is not found
                    }

                    try {
                        // Parse the game URL to extract domain and token
                        java.net.URL url = new java.net.URL(gameUrl);
                        String domain = url.getProtocol() + "://" + url.getHost();  // Extract protocol and domain from the URL
                        String token = gameUrl.substring(gameUrl.indexOf("token=") + 6, gameUrl.indexOf("&loader"));  // Extract token from the URL
                        String path = "/launcherconfig/" + token + ".txt";  // Construct the path for the launcher config file

                        // Build the full URL dynamically
                        String builtUrl = domain + path;
                        System.out.println("Built URL: " + builtUrl);

                        // Save the built URL, token, and domain to the session
                        return session.set("builtUrl", builtUrl).set("gameToken", token).set("domain", domain);
                    } catch (Exception e) {
                        // Log any errors encountered while parsing the URL
                        System.err.println("Error parsing gameUrl: " + e.getMessage());
                        exitHereIfFailed();  // Exit if URL parsing fails
                    }

                    return session;
                })

                // Step 3: Send a GET request to download the launcher config file
                .exec(http("Download Text File")
                        .get("#{builtUrl}")  // Use the dynamically built URL for the download request
                        .header("accept", "*/*")  // Accept all response types
                        .header("accept-encoding", "gzip, deflate, br, zstd")  // Accept-Encoding header for the response
                        .header("accept-language", "en-US,en;q=0.9")  // Accept-Language header
                        .header("priority", "u=1, i")  // Priority header
                        .header("referer", "#{origin}")  // Referer header from the session
                        .header("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")  // sec-ch-ua header for the browser version
                        .header("sec-ch-ua-mobile", "?0")  // sec-ch-ua-mobile header
                        .header("sec-ch-ua-platform", "\"Windows\"")  // sec-ch-ua-platform header
                        .header("sec-fetch-dest", "empty")  // sec-fetch-dest header
                        .header("sec-fetch-mode", "cors")  // sec-fetch-mode header
                        .header("sec-fetch-site", "same-origin")  // sec-fetch-site header
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")  // User-Agent header
                        .check(status().is(200))  // Check if the download is successful (status code 200)
                        .check(bodyString().saveAs("downloadedContent"))  // Save the downloaded content to the session
                )

                // Step 4: Process the downloaded content and extract necessary information
                .exec(session -> {
                    String downloadedContent = session.getString("downloadedContent");  // Retrieve the downloaded content from session

                    if (downloadedContent == null || downloadedContent.isEmpty()) {
                        // If the downloaded content is empty, log an error and exit
                        System.err.println("Error: Downloaded content is empty!");
                        exitHereIfFailed();  // Exit if downloaded content is empty
                    }

                    System.out.println("Downloaded Content: " + downloadedContent);

                    try {
                        // Decode the downloaded content (Base64)
                        byte[] decodedOpt = Base64.getDecoder().decode(downloadedContent);
                        String decodedOptString = new String(decodedOpt);  // Convert the decoded byte array to a string
                        System.out.println("Decoded Opt String: " + decodedOptString);

                        // Parse the JSON object from the decoded string
                        JSONObject jsonObject1 = new JSONObject(decodedOptString);
                        String encodedParams = jsonObject1.getString("params");
                        String decodedParams = new String(Base64.getDecoder().decode(encodedParams));  // Decode 'params'
                        System.out.println("Decoded Params: " + decodedParams);

                        // Parse the second JSON object and extract the session key
                        JSONObject jsonObject2 = new JSONObject(decodedParams);
                        String gamesessionKey = jsonObject2.getString("sessionId");
                        System.out.println("gamesessionKey: " + gamesessionKey);

                        // Save the session key to the session
                        return session.set("gamesessionKey", gamesessionKey);

                    } catch (Exception e) {
                        // Log any errors encountered during decoding or JSON parsing
                        System.err.println("Error during decoding or JSON parsing: " + e.getMessage());
                        exitHereIfFailed();  // Exit if an error occurs
                    }

                    return session;
                })

                // Uncomment below lines if you need to track the ready users count and wait for target users
//        .exec(session -> {
//            // Update the readyUsers counter
//            int currentCount = readyUsers.incrementAndGet();
//            System.out.println("Ready users count: " + currentCount);
//            return session;
//        })
//        // Wait-until logic
//        .asLongAs(session -> readyUsers.get() < targetReadyUsers)
//        .on(
//                exec(session -> {
//                    System.out.println("User waiting. Current readyUsers: "
//                            + readyUsers.get() + ", Target: " + targetReadyUsers);
//                    return session;
//                })
//                        .pause(1) // Wait 1 second before rechecking
//        )

                // Step 5: Exit if any of the previous steps fail
                .exitHereIfFailed();  // Exit if any of the checks fail
    }
}
