package CsrfFetcher;

import io.gatling.javaapi.core.ChainBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Class responsible for handling CSRF token fetching in Gatling performance tests.
 * This class includes logic to make a GET request to the provided base URL and extract the CSRF token
 * from the HTML response using a regex pattern.
 */
public class FetchCsrf {

    /**
     * Fetches the CSRF token from the base URL by making an HTTP GET request.
     *
     * @param baseurl The base URL from which the CSRF token will be fetched.
     * @return A ChainBuilder that executes the CSRF request and saves the token for use in subsequent requests.
     */
    public static ChainBuilder fetchCsrfrequest(String baseurl) {
        return exec(http("Get CSRF Token") // HTTP request to get CSRF token
                .get(baseurl) // Perform a GET request to the provided base URL
                .check(status().is(200)) // Ensure the status code is 200 (OK)
                .check(regex("name=\"csrf_token\" value=\"([^\"]+)\"") // Extract CSRF token from the HTML response using regex
                        .saveAs("CSRFTOKEN")) // Save the CSRF token as a session variable for use in future requests
        )
                .exitHereIfFailed(); // Exit the chain if any step fails (such as a failed response)
    }
}
