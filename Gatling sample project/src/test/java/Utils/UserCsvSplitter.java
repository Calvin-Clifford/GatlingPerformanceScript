package Utils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UserCsvSplitter {

    // Directory where the CSV files will be stored
    private static final String TEMP_FOLDER = "target/test-classes";
    // Map to store the user data for each server instance
    private static final Map<String, List<Map<String, String>>> userMap = new HashMap<>();

    /**
     * Generates user chunks by dividing users into smaller groups and saving them into CSV files.
     *
     * @param startUser       The username where the user list starts.
     * @param endUser         The username where the user list ends.
     * @param numScenarios    The number of scenarios to divide the users into.
     * @param totalInstances  The total number of server instances.
     */
    public static void generateUserChunks(String startUser, String endUser, int numScenarios, int totalInstances) {
        // Ensure the temporary folder exists for storing CSV files
        ensureTempFolderExists();

        // Extract the prefix and numeric parts of the start and end user
        String prefix = extractPrefix(startUser);
        int start = extractNumericPart(startUser);
        int end = extractNumericPart(endUser);

        // Generate a list of users with usernames and default passwords
        List<Map<String, String>> users = IntStream.rangeClosed(start, end)
                .mapToObj(i -> {
                    Map<String, String> userWithPassword = new HashMap<>();
                    String username = prefix + i;
                    userWithPassword.put("username", username);
                    userWithPassword.put("password", "Test@123");
                    return userWithPassword;
                })
                .collect(Collectors.toList());

        int totalUsers = users.size();
        int usersPerInstance = totalUsers / totalInstances;

        // Get the current server ID from system properties
        String currentServerId = System.getProperty("server.identifier", "server1");

        // Partition the users into separate server instances
        for (int instanceId = 1; instanceId <= totalInstances; instanceId++) {
            int instanceStartIndex = (instanceId - 1) * usersPerInstance;
            int instanceEndIndex = (instanceId == totalInstances) ? totalUsers : instanceStartIndex + usersPerInstance;

            List<Map<String, String>> serverUsers = users.subList(instanceStartIndex, instanceEndIndex);
            String serverId = "server" + instanceId;
            userMap.put(serverId, serverUsers);

            // Skip other server instances
            if (!serverId.equals(currentServerId)) {
                continue;
            }

            // Split users into smaller chunks for the scenarios
            List<List<Map<String, String>>> userChunks = splitUsers(serverUsers, numScenarios);

            // Write each chunk to a separate CSV file
            for (int scenarioIndex = 0; scenarioIndex < userChunks.size(); scenarioIndex++) {
                String fileName = generateFileName(serverId, scenarioIndex + 1);
                writeUsersToCsv(userChunks.get(scenarioIndex), fileName);
            }
        }
    }

    /**
     * Generates the CSV file name based on server ID and scenario index.
     *
     * @param serverId      The server ID.
     * @param scenarioIndex The index of the scenario.
     * @return The generated CSV file name.
     */
    private static String generateFileName(String serverId, int scenarioIndex) {
        int serverNumber = extractNumericPart(serverId);
        return TEMP_FOLDER + File.separator + "s" + serverNumber + "_" + scenarioIndex + ".csv";
    }

    /**
     * Writes the list of users to a CSV file.
     *
     * @param users     The list of users to write.
     * @param fileName  The name of the file where users will be saved.
     */
    private static void writeUsersToCsv(List<Map<String, String>> users, String fileName) {
        try (FileWriter writer = new FileWriter(fileName, false)) {
            writer.append("username,password\n");
            for (Map<String, String> user : users) {
                writer.append(user.get("username")).append(",").append(user.get("password")).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + fileName);
            e.printStackTrace();
        }
    }

    /**
     * Splits the list of users into smaller chunks for the scenarios.
     *
     * @param users          The list of users.
     * @param numScenarios   The number of scenarios to divide the users into.
     * @return A list of user chunks.
     */
    private static List<List<Map<String, String>>> splitUsers(List<Map<String, String>> users, int numScenarios) {
        int usersPerScenario = users.size() / numScenarios;
        List<List<Map<String, String>>> userChunks = new ArrayList<>();
        for (int i = 0; i < numScenarios; i++) {
            int fromIndex = i * usersPerScenario;
            int toIndex = (i == numScenarios - 1) ? users.size() : fromIndex + usersPerScenario;
            userChunks.add(users.subList(fromIndex, toIndex));
        }
        return userChunks;
    }

    /**
     * Ensures that the temporary folder exists. If it doesn't, it attempts to create it.
     */
    private static void ensureTempFolderExists() {
        File tempFolder = new File(TEMP_FOLDER);
        if (!tempFolder.exists() && !tempFolder.mkdir()) {
            throw new RuntimeException("Failed to create directory: " + TEMP_FOLDER);
        }
    }

    /**
     * Retrieves the CSV file name for a specific scenario and server ID.
     *
     * @param scenarioIndex The scenario index.
     * @param serverId      The server ID.
     * @return The name of the CSV file.
     */
    public static String getCsvFileForScenario(int scenarioIndex, String serverId) {
        int serverNumber = extractNumericPart(serverId);
        return String.format("s%s_%d.csv", serverNumber, scenarioIndex);
    }

    /**
     * Extracts the numeric part from a string (e.g., "user123" will return 123).
     *
     * @param str The input string.
     * @return The numeric part of the string.
     */
    private static int extractNumericPart(String str) {
        String numericPart = str.replaceFirst(".*\\D(\\d+)", "$1");
        return Integer.parseInt(numericPart);
    }

    /**
     * Extracts the prefix from a string before the numeric part.
     *
     * @param str The input string.
     * @return The prefix of the string.
     */
    private static String extractPrefix(String str) {
        return str.replaceFirst("(.*\\D)\\d+.*", "$1");
    }

    /**
     * Retrieves the list of users for a specific server.
     *
     * @param serverId      The server ID.
     * @param totalInstances The total number of server instances.
     * @return The list of users for the server.
     */
    public static List<Map<String, String>> getUsersForScenario(String serverId, int totalInstances) {
        if (!userMap.containsKey(serverId)) {
            throw new RuntimeException("No users found for server: " + serverId);
        }
        return userMap.get(serverId);
    }

    /**
     * Prints the details of the user chunks for each server.
     */
    public static void printChunkDetails() {
        userMap.forEach((server, users) -> {
            System.out.println("Server: " + server + ", Number of users: " + users.size());
        });
    }

    /**
     * Deletes all CSV files in the temp folder.
     */
    public static void deleteAllCsvFiles() {
        File folder = new File("target/test-classes");
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("The directory does not exist: " + folder.getAbsolutePath());
            return;
        }

        File[] csvFiles = folder.listFiles((dir, name) -> name.endsWith(".csv"));
        if (csvFiles != null && csvFiles.length > 0) {
            for (File file : csvFiles) {
                if (file.delete()) {
                    System.out.println("Deleted: " + file.getName());
                } else {
                    System.err.println("Failed to delete: " + file.getName());
                }
            }
        } else {
            System.out.println("No CSV files found in the directory: " + folder.getAbsolutePath());
        }
    }

    /**
     * Retrieves the first CSV file from the specified directory.
     *
     * @param resourcesDir The directory containing the CSV files.
     * @return The first CSV file found.
     */
    public static File getCsvFileFromResources(String resourcesDir) {
        // Define the directory as a File object
        File directory = new File(resourcesDir);

        // Check if the directory exists and is a valid directory
        if (!directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("The directory does not exist or is not a directory: " + resourcesDir);
        }

        // List all CSV files in the directory
        File[] csvFiles = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv");  // Accept files with .csv extension
            }
        });

        // Check if any .csv files are found
        if (csvFiles == null || csvFiles.length == 0) {
            throw new RuntimeException("No .csv files found in directory: " + resourcesDir);
        }

        // If .csv files are found, return the first one
        System.out.println("Found CSV files:");
        for (File csvFile : csvFiles) {
            System.out.println(csvFile.getName());
        }

        return csvFiles[0]; // Returning the first CSV file found
    }
}
