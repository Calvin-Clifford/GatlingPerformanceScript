package Registration;

import Utils.Support;
import io.gatling.javaapi.core.ChainBuilder;
import java.util.concurrent.atomic.AtomicInteger;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Registration {

    /**
     * Handles the registration request and checks user readiness.
     *
     * @param baseurl            The base URL for the API.
     * @param signupendpoint     The specific signup endpoint to make the POST request to.
     * @param bonuscode          The bonus code to be passed during registration.
     * @param csvStore           The CSV file containing user data for registration.
     * @param readyUsers         Atomic integer counter for ready users.
     * @param targetReadyUsers   The target number of users to be ready.
     * @return A ChainBuilder representing the steps for the registration request.
     */
    public static ChainBuilder registrationRequest(String baseurl, String signupendpoint, String bonuscode, String csvStore, AtomicInteger readyUsers, int targetReadyUsers) {
        return feed(csv(csvStore)) // Feed data from CSV file to simulate user registration
                .exec(session -> {
                    // Step 1: Generate and Load Email ID for the new user
                    String EmailID = Support.generateRandomEmail();
                    // Store Email ID, origin, and bonuscode in the session
                    return session.set("EmailID", EmailID).set("origin", baseurl).set("bonuscode", bonuscode);
                })
                .exec(http("Registration") // Make a POST request to the registration endpoint
                        .post(baseurl + signupendpoint) // Full URL formed by concatenating base URL and signup endpoint
                        .header("Accept", "application/json, text/javascript, */*; q=0.01")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.70 Safari/537.36")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Origin", "#{origin}") // Dynamic origin set in the session
                        .header("Referer", "#{origin}") // Referer header for the origin
                        .header("Sec-Fetch-Site", "same-origin")
                        .header("Sec-Fetch-Mode", "cors")
                        .header("Sec-Fetch-Dest", "empty")
                        .formParam("BaseUrl", baseurl) // Send base URL as form parameter
                        .formParam("campaign_code", "") // Empty parameter
                        .formParam("rafUserId", "") // Empty parameter
                        .formParam("rafcampaign_code", "") // Empty parameter
                        .formParam("affiliate_tagcode", "sdfsdfsdfsdf") // Affiliate tagcode value
                        .formParam("device_screen", "422*1536") // Device screen size
                        .formParam("csrf_token", "#{CSRFTOKEN}") // CSRF token to prevent CSRF attacks
                        .formParam("Username", "#{username}") // Username parameter
                        .formParam("UserPassword", "#{password}") // Password parameter
                        .formParam("UserEmail", "#{EmailID}") // Email parameter
                        .formParam("user_dob", "14-11-2006") // Date of birth
                        .formParam("UserCurrencyCode", "USD") // Currency code
                        .formParam("affiliate_tagcode", "#{bonuscode}") // Bonus code for referral
                        .formParam("agecnfrm", "on") // Age confirmation checkbox
                        .formParam("UserAgreeCheckbox", "on") // Agreement checkbox for user
                        .formParam("csrf_token", "#{CSRFTOKEN}") // Another CSRF token (intentional redundancy)
                        .check(status().is(200)) // Ensure a successful response (200 status)
                        .check(jsonPath("$.error_message").is("Registration Successfully!!!")) // Verify registration success message
                )
                .exec(session -> {
                    // Step 2: Update the readyUsers counter after successful registration
                    int currentCount = readyUsers.incrementAndGet();
                    System.out.println("Ready users count: " + currentCount);
                    return session;
                })
                // Step 3: Use 'asLongAs' to wait until the desired number of ready users is reached
                .asLongAs(session -> readyUsers.get() < targetReadyUsers)
                .on(
                        exec(session -> {
                            // Display current status of ready users
                            System.out.println("User waiting. Current readyUsers: "
                                    + readyUsers.get() + ", Target: " + targetReadyUsers);
                            return session;
                        })
                                .pause(1) // Pause for 1 second before rechecking user readiness
                )
                .exitHereIfFailed(); // Exit the chain if any request fails
    }
}
