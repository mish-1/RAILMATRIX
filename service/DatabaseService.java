package service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private static final String DB_URL = envOrDefault("RAILMATRIX_DB_URL", "jdbc:mysql://localhost:3306/railmatrix");
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

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS bookings ("
                   + "booking_id INT PRIMARY KEY AUTO_INCREMENT, "
                   + "user_id INT NOT NULL, "
                   + "user_name VARCHAR(100) NOT NULL, "
                   + "train_id INT NOT NULL, "
                   + "train_name VARCHAR(100) NOT NULL, "
                   + "source VARCHAR(100) NOT NULL, "
                   + "destination VARCHAR(100) NOT NULL"
                   + ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
        }
    }

    public void initializeTrainSearchSchema() {
        String createStation = "CREATE TABLE IF NOT EXISTS STATION ("
                + "station_id INT PRIMARY KEY, "
            + "station_code VARCHAR(10) NOT NULL, "
            + "station_name VARCHAR(100) NOT NULL UNIQUE, "
            + "state VARCHAR(50), "
            + "zone VARCHAR(50), "
            + "station_type VARCHAR(20)"
                + ")";

        String createTrain = "CREATE TABLE IF NOT EXISTS TRAIN ("
                + "train_id INT PRIMARY KEY, "
            + "train_number VARCHAR(20) NOT NULL, "
            + "train_name VARCHAR(100) NOT NULL, "
            + "train_type VARCHAR(50), "
            + "days_of_run VARCHAR(50)"
                + ")";

        String createRoute = "CREATE TABLE IF NOT EXISTS ROUTE ("
            + "route_id INT PRIMARY KEY, "
                + "train_id INT NOT NULL, "
                + "station_id INT NOT NULL, "
                + "stop_number INT NOT NULL, "
            + "arrival_time TIME NULL, "
            + "departure_time TIME NULL, "
            + "halt_duration INT NULL, "
            + "distance_from_source INT NULL, "
                + "FOREIGN KEY (train_id) REFERENCES TRAIN(train_id), "
                + "FOREIGN KEY (station_id) REFERENCES STATION(station_id)"
                + ")";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(createStation);
            statement.executeUpdate(createTrain);
            statement.executeUpdate(createRoute);

                seedTrainSearchData(statement);
        } catch (SQLException e) {
            System.out.println("Train schema setup failed: " + e.getMessage());
        }
    }

    private void seedTrainSearchData(Statement statement) throws SQLException {
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 901, 'R901', 'Mumbai', 'Maharashtra', 'WR', 'Terminal' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'mumbai%')");
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 902, 'R902', 'Lonavala', 'Maharashtra', 'CR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'lonavala%')");
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 903, 'R903', 'Pune', 'Maharashtra', 'CR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'pune%')");
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 904, 'R904', 'Bangalore', 'Karnataka', 'SWR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'bangalore%' OR LOWER(TRIM(station_name)) LIKE 'bengaluru%')");
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 905, 'R905', 'Chennai', 'Tamil Nadu', 'SR', 'Terminal' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'chennai%')");
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 906, 'R906', 'Delhi', 'Delhi', 'NR', 'Terminal' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'delhi%')");
        statement.executeUpdate("INSERT INTO STATION (station_id, station_code, station_name, state, zone, station_type) "
            + "SELECT 907, 'R907', 'Jaipur', 'Rajasthan', 'NWR', 'Junction' FROM DUAL "
            + "WHERE NOT EXISTS (SELECT 1 FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'jaipur%')");

        statement.executeUpdate("INSERT INTO TRAIN (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 901, '12001', 'RedLine Express', 'Express', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM TRAIN WHERE train_id = 901)");
        statement.executeUpdate("INSERT INTO TRAIN (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 902, '12002', 'Night Rider', 'Superfast', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM TRAIN WHERE train_id = 902)");
        statement.executeUpdate("INSERT INTO TRAIN (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 903, '12003', 'Coastal Runner', 'Express', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM TRAIN WHERE train_id = 903)");
        statement.executeUpdate("INSERT INTO TRAIN (train_id, train_number, train_name, train_type, days_of_run) "
            + "SELECT 904, '12004', 'Western Link', 'Intercity', 'Daily' FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM TRAIN WHERE train_id = 904)");

        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
            + "SELECT 9001, 901, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) = 'mumbai' LIMIT 1), 1, NULL, '06:00:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 901 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9002, 901, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'lonavala%' LIMIT 1), 2, '08:00:00', '08:05:00', 5, 110 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 901 AND stop_number = 2)");
        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9003, 901, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'pune%' LIMIT 1), 3, '09:30:00', NULL, 0, 180 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 901 AND stop_number = 3)");

        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9004, 902, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'delhi%' LIMIT 1), 1, NULL, '22:00:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 902 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9005, 902, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'jaipur%' LIMIT 1), 2, '03:00:00', NULL, 0, 280 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 902 AND stop_number = 2)");

        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9006, 903, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'pune%' LIMIT 1), 1, NULL, '10:00:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 903 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9007, 903, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'bangalore%' OR LOWER(TRIM(station_name)) LIKE 'bengaluru%' LIMIT 1), 2, '18:00:00', '18:10:00', 10, 840 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 903 AND stop_number = 2)");
        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9008, 903, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'chennai%' LIMIT 1), 3, '23:00:00', NULL, 0, 1200 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 903 AND stop_number = 3)");

        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9009, 904, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'mumbai%' LIMIT 1), 1, NULL, '07:30:00', 0, 0 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 904 AND stop_number = 1)");
        statement.executeUpdate("INSERT INTO ROUTE (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source) "
                + "SELECT 9010, 904, (SELECT station_id FROM STATION WHERE LOWER(TRIM(station_name)) LIKE 'bangalore%' OR LOWER(TRIM(station_name)) LIKE 'bengaluru%' LIMIT 1), 2, '22:00:00', NULL, 0, 980 FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM ROUTE WHERE train_id = 904 AND stop_number = 2)");
    }
}
