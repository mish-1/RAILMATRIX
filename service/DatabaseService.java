package service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseService {

    private static final String DB_URL = envOrDefault("RAILMATRIX_DB_URL", inferDefaultDbUrl());
    private static final String DB_USER = envOrDefault("RAILMATRIX_DB_USER", "root");
    private static final String DB_PASSWORD = envOrDefault("RAILMATRIX_DB_PASSWORD", "root");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found in classpath.", e);
        }
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String inferDefaultDbUrl() {
        String defaultDbName = "ConnectingTrainDB";
        String dbNameFromSql = findDatabaseNameInSql(defaultDbName);
        return "jdbc:mysql://localhost:3306/" + dbNameFromSql;
    }

    private static String findDatabaseNameInSql(String fallbackDbName) {
        Path sqlPath = Path.of("railmatrix.sql");
        if (!Files.exists(sqlPath)) {
            return fallbackDbName;
        }

        Pattern createDbPattern = Pattern.compile("(?i)^\\s*CREATE\\s+DATABASE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([A-Za-z0-9_]+)`?\\s*;.*$");

        try {
            List<String> lines = Files.readAllLines(sqlPath);
            for (String line : lines) {
                Matcher matcher = createDbPattern.matcher(line);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception ignored) {
            // Keep fallback DB name when sql parsing fails.
        }

        return fallbackDbName;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void initializeSchema() {
        String createUser = "CREATE TABLE IF NOT EXISTS `User` ("
                + "user_id INT PRIMARY KEY, "
                + "user_name VARCHAR(50) NOT NULL, "
                + "email VARCHAR(100) UNIQUE NOT NULL, "
                + "phone_number VARCHAR(15) NOT NULL"
                + ")";

        String createBooking = "CREATE TABLE IF NOT EXISTS `Booking` ("
                + "booking_id INT PRIMARY KEY AUTO_INCREMENT, "
                + "user_id INT NOT NULL, "
                + "train_id INT NOT NULL, "
                + "source_station_id INT NOT NULL, "
                + "destination_station_id INT NOT NULL, "
                + "journey_date DATE NOT NULL, "
                + "booking_date DATE NOT NULL, "
                + "seat_count INT NOT NULL, "
                + "booking_status VARCHAR(20) NOT NULL DEFAULT 'Confirmed', "
                + "FOREIGN KEY (user_id) REFERENCES `User`(user_id), "
                + "FOREIGN KEY (train_id) REFERENCES `Train`(train_id), "
                + "FOREIGN KEY (source_station_id) REFERENCES `Station`(station_id), "
                + "FOREIGN KEY (destination_station_id) REFERENCES `Station`(station_id)"
                + ")";

        String createTicket = "CREATE TABLE IF NOT EXISTS `Ticket` ("
                + "ticket_id INT PRIMARY KEY AUTO_INCREMENT, "
                + "booking_id INT NOT NULL, "
                + "passenger_name VARCHAR(100) NOT NULL, "
                + "coach_no VARCHAR(10), "
                + "seat_no VARCHAR(10), "
                + "fare DECIMAL(8,2), "
                + "FOREIGN KEY (booking_id) REFERENCES `Booking`(booking_id)"
                + ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createUser);
            statement.executeUpdate(createBooking);
            statement.executeUpdate(createTicket);
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
        }
    }

    public void initializeTrainSearchSchema() {
        String createStation = "CREATE TABLE IF NOT EXISTS `Station` ("
                + "station_id INT PRIMARY KEY, "
                + "station_code VARCHAR(10) NOT NULL UNIQUE, "
                + "station_name VARCHAR(100) NOT NULL, "
                + "state VARCHAR(50), "
                + "zone VARCHAR(20), "
                + "station_type VARCHAR(20)"
                + ")";

        String createTrain = "CREATE TABLE IF NOT EXISTS `Train` ("
                + "train_id INT PRIMARY KEY, "
                + "train_number INT NOT NULL UNIQUE, "
                + "train_name VARCHAR(100) NOT NULL, "
                + "train_type VARCHAR(50), "
                + "days_of_run VARCHAR(50)"
                + ")";

        String createRoute = "CREATE TABLE IF NOT EXISTS `Route` ("
                + "route_id INT PRIMARY KEY, "
                + "train_id INT NOT NULL, "
                + "station_id INT NOT NULL, "
                + "stop_number INT NOT NULL, "
                + "arrival_time TIME NULL, "
                + "departure_time TIME NULL, "
                + "halt_duration INT NULL, "
                + "distance_from_source INT NULL, "
                + "FOREIGN KEY (train_id) REFERENCES `Train`(train_id), "
                + "FOREIGN KEY (station_id) REFERENCES `Station`(station_id)"
                + ")";

        String createStop = "CREATE TABLE IF NOT EXISTS `Stop` ("
                + "stop_id INT PRIMARY KEY AUTO_INCREMENT, "
                + "route_id INT NOT NULL, "
                + "station_id INT NOT NULL, "
                + "stop_sequence INT NOT NULL, "
                + "arrival_time TIME NULL, "
                + "departure_time TIME NULL, "
                + "halt_duration INT NULL, "
                + "FOREIGN KEY (route_id) REFERENCES `Route`(route_id), "
                + "FOREIGN KEY (station_id) REFERENCES `Station`(station_id)"
                + ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(createStation);
            statement.executeUpdate(createTrain);
            statement.executeUpdate(createRoute);
            statement.executeUpdate(createStop);

            seedTrainSearchData(statement);
        } catch (SQLException e) {
            System.out.println("Train schema setup failed: " + e.getMessage());
        }
    }

    private void seedTrainSearchData(Statement statement) throws SQLException {
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 901, 'R901', 'Mumbai', 'Maharashtra', 'WR', 'Terminal' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'mumbai%')");
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 902, 'R902', 'Lonavala', 'Maharashtra', 'CR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'lonavala%')");
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 903, 'R903', 'Pune', 'Maharashtra', 'CR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'pune%')");
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 904, 'R904', 'Bangalore', 'Karnataka', 'SWR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'bangalore%' OR LOWER(TRIM(station_name)) LIKE 'bengaluru%')");
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 905, 'R905', 'Chennai', 'Tamil Nadu', 'SR', 'Terminal' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'chennai%')");
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 906, 'R906', 'Delhi', 'Delhi', 'NR', 'Terminal' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'delhi%')");
        statement.executeUpdate("INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 907, 'R907', 'Jaipur', 'Rajasthan', 'NWR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'jaipur%')");

        statement.executeUpdate("INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 901, 12001, 'RedLine Express', 'Express', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 901)");
        statement.executeUpdate("INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 902, 12002, 'Night Rider', 'Superfast', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 902)");
        statement.executeUpdate("INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 903, 12003, 'Coastal Runner', 'Express', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 903)");
        statement.executeUpdate("INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 904, 12004, 'Western Link', 'Intercity', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 904)");

        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
            + "SELECT 9001, 901, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) = 'mumbai' LIMIT 1), 1, NULL, '06:00:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 901 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9002, 901, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'lonavala%' LIMIT 1), 2, '08:00:00', '08:05:00', 5, 110 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 901 AND stop_number = 2)");
        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9003, 901, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'pune%' LIMIT 1), 3, '09:30:00', NULL, 0, 180 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 901 AND stop_number = 3)");

        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9004, 902, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'delhi%' LIMIT 1), 1, NULL, '22:00:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 902 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9005, 902, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'jaipur%' LIMIT 1), 2, '03:00:00', NULL, 0, 280 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 902 AND stop_number = 2)");

        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9006, 903, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'pune%' LIMIT 1), 1, NULL, '10:00:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 903 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9007, 903, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'bangalore%' OR LOWER(TRIM(station_name)) LIKE 'bengaluru%' LIMIT 1), 2, '18:00:00', '18:10:00', 10, 840 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 903 AND stop_number = 2)");
        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9008, 903, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'chennai%' LIMIT 1), 3, '23:00:00', NULL, 0, 1200 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 903 AND stop_number = 3)");

        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9009, 904, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'mumbai%' LIMIT 1), 1, NULL, '07:30:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 904 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9010, 904, (SELECT station_id FROM `Station` WHERE LOWER(TRIM(station_name)) LIKE 'bangalore%' OR LOWER(TRIM(station_name)) LIKE 'bengaluru%' LIMIT 1), 2, '22:00:00', NULL, 0, 980 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE train_id = 904 AND stop_number = 2)");
    }
}
