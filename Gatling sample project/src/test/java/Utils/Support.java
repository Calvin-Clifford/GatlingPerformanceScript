package Utils;

import java.util.Random;

public class Support {

    /**
     * Generates a random email address.
     * The email address consists of a randomly generated username (5-10 characters)
     * and a random domain from a predefined list of email providers.
     *
     * @return A randomly generated email address.
     */
    public static String generateRandomEmail() {
        // Create an instance of Random for generating random numbers
        Random random = new Random();

        // Predefined list of email providers
        String[] emailProviders = { "gmail.com", "yahoo.com", "hotmail.com", "outlook.com" };

        // Alphabet list for generating username
        String alphabets = "abcdefghijklmnopqrstuvwxyz";

        // StringBuilder to hold the randomly generated username
        StringBuilder username = new StringBuilder();

        // Randomly determine the length of the username (between 5 and 10 characters)
        int usernameLength = random.nextInt(6) + 5;

        // Generate the username by selecting random characters from the alphabet
        for (int i = 0; i < usernameLength; i++) {
            // Pick a random character from the alphabet and append it to the username
            char randomChar = alphabets.charAt(random.nextInt(alphabets.length()));
            username.append(randomChar);
        }

        // Randomly pick an email provider from the list
        String emailProvider = emailProviders[random.nextInt(emailProviders.length)];

        // Combine the username and email provider to form the full email address
        return username.toString() + "@" + emailProvider;
    }
}
