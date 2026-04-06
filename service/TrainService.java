package service;

import model.Train;

import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class TrainService {

    private final Scanner scanner;
    private final DatabaseService databaseService;
    private final ArrayList<Train> trains = new ArrayList<>();

    public TrainService(Scanner scanner) {
        this.scanner = scanner;
        this.databaseService = new DatabaseService();
        this.databaseService.initializeTrainSearchSchema();

        // Local train list keeps booking flow stable even if route tables are missing.
        trains.add(new Train(901, "RedLine Express", "Mumbai", "Pune"));
        trains.add(new Train(902, "Night Rider", "Delhi", "Jaipur"));
        trains.add(new Train(903, "Coastal Runner", "Pune", "Chennai"));
        trains.add(new Train(904, "Western Link", "Mumbai", "Bangalore"));
    }

    public void displayTrains() {
        String sql = "SELECT train_id, train_name FROM `Train` ORDER BY train_id";

        try (Connection con = databaseService.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(rs.getInt("train_id") + " - " + rs.getString("train_name"));
            }

            if (!found) {
                for (Train train : trains) {
                    System.out.println(train);
                }
            }
        } catch (SQLException e) {
            for (Train train : trains) {
                System.out.println(train);
            }
        }
    }

    public Train getTrainById(int id) {
        String sql = "SELECT t.train_id, t.train_name, "
            + "(SELECT s.station_name FROM `Route` r JOIN `Station` s ON s.station_id = r.station_id "
            + " WHERE r.train_id = t.train_id ORDER BY r.stop_number ASC LIMIT 1) AS source_station, "
            + "(SELECT s.station_name FROM `Route` r JOIN `Station` s ON s.station_id = r.station_id "
            + " WHERE r.train_id = t.train_id ORDER BY r.stop_number DESC LIMIT 1) AS destination_station "
            + "FROM `Train` t WHERE t.train_id = ?";

        try (Connection con = databaseService.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String source = rs.getString("source_station");
                    String destination = rs.getString("destination_station");
                    if (source == null || source.isBlank()) {
                        source = "Unknown";
                    }
                    if (destination == null || destination.isBlank()) {
                        destination = "Unknown";
                    }
                    return new Train(rs.getInt("train_id"), rs.getString("train_name"), source, destination);
                }
            }
        } catch (SQLException e) {
            // Fall back to local cache when DB is unavailable.
        }

        for (Train train : trains) {
            if (train.getTrainId() == id) {
                return train;
            }
        }
        return null;
    }

    public void searchTrains(String source, String destination) {

        source = source.trim();
        destination = destination.trim();

        if (source.isEmpty() || destination.isEmpty()) {
            System.out.println("Source and destination cannot be empty.");
            return;
        }

        try (Connection con = databaseService.getConnection()) {

            // ==========================
            // 1. DIRECT TRAINS
            // ==========================
            System.out.println("\n--- Direct Trains ---");

            String directQuery =
                    "SELECT DISTINCT t.train_id, t.train_name " +
                    "FROM `Route` r1 " +
                    "JOIN `Route` r2 ON r1.train_id = r2.train_id " +
                    "JOIN `Train` t ON t.train_id = r1.train_id " +
                    "JOIN `Station` s1 ON r1.station_id = s1.station_id " +
                    "JOIN `Station` s2 ON r2.station_id = s2.station_id " +
                    "WHERE LOWER(TRIM(s1.station_name)) LIKE ? " +
                    "AND LOWER(TRIM(s2.station_name)) LIKE ? " +
                    "AND r1.stop_number < r2.stop_number";

            boolean foundDirect = false;

            try (PreparedStatement ps1 = con.prepareStatement(directQuery)) {
                ps1.setString(1, source.toLowerCase() + "%");
                ps1.setString(2, destination.toLowerCase() + "%");

                try (ResultSet rs1 = ps1.executeQuery()) {
                    while (rs1.next()) {
                        foundDirect = true;
                        System.out.println("Train ID: " + rs1.getInt("train_id") +
                                           " | " + rs1.getString("train_name"));
                    }
                }
            }

            if (!foundDirect) {
                System.out.println("No Direct Trains Found.");
            }

            // ==========================
            // 2. CONNECTING TRAINS
            // ==========================
            System.out.println("\n--- Connecting Trains ---");

            String connectQuery =
                    "SELECT DISTINCT " +
                    "t1.train_id AS t1_id, t1.train_name AS first_train, " +
                    "t2.train_id AS t2_id, t2.train_name AS second_train, " +
                    "sj.station_name AS junction " +
                    "FROM `Route` r1 " +
                    "JOIN `Route` rj1 ON r1.train_id = rj1.train_id " +
                    "JOIN `Route` rj2 ON rj1.station_id = rj2.station_id " +
                    "JOIN `Route` r2 ON r2.train_id = rj2.train_id " +
                    "JOIN `Train` t1 ON t1.train_id = r1.train_id " +
                    "JOIN `Train` t2 ON t2.train_id = r2.train_id " +
                    "JOIN `Station` s1 ON r1.station_id = s1.station_id " +
                    "JOIN `Station` s2 ON r2.station_id = s2.station_id " +
                    "JOIN `Station` sj ON rj1.station_id = sj.station_id " +
                    "WHERE LOWER(TRIM(s1.station_name)) LIKE ? " +
                    "AND LOWER(TRIM(s2.station_name)) LIKE ? " +
                    "AND r1.stop_number < rj1.stop_number " +
                    "AND rj2.stop_number < r2.stop_number " +
                    "AND r1.train_id <> r2.train_id";

            boolean foundConnection = false;

            try (PreparedStatement ps2 = con.prepareStatement(connectQuery)) {
                ps2.setString(1, source.toLowerCase() + "%");
                ps2.setString(2, destination.toLowerCase() + "%");

                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        foundConnection = true;
                        System.out.println(
                                "Train " + rs2.getString("first_train") +
                                " -> [" + rs2.getString("junction") + "] -> " +
                                rs2.getString("second_train")
                        );
                    }
                }
            }

            if (!foundConnection) {
                System.out.println("No Connecting Trains Found.");
            }

        } catch (SQLException e) {
            System.out.println("Train search failed: " + e.getMessage());
        }
    }
}