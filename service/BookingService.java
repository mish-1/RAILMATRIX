package service;

import model.*;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BookingService implements BookingOperations {

    private TrainService trainService;
    private Scanner scanner;
    private DatabaseService databaseService;

    public BookingService(TrainService trainService, Scanner scanner) {
        this.trainService = trainService;
        this.scanner = scanner;
        this.databaseService = new DatabaseService();
        this.databaseService.initializeSchema();
    }

    @Override
    public void bookTicket() {

        try {
            System.out.print("Enter User ID: ");
            int userId = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter User Name: ");
            String name = scanner.nextLine();

            User user = new User(userId, name);

            System.out.print("Enter Train ID to book: ");
            int trainId = Integer.parseInt(scanner.nextLine());

            Train train = trainService.getTrainById(trainId);

            if (train == null) {
                System.out.println("Invalid Train ID!");
                return;
            }

            String sql = "INSERT INTO bookings (user_id, user_name, train_id, train_name, source, destination) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection connection = databaseService.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                statement.setInt(1, user.getUserId());
                statement.setString(2, user.getName());
                statement.setInt(3, train.getTrainId());
                statement.setString(4, train.getTrainName());
                statement.setString(5, train.getSource());
                statement.setString(6, train.getDestination());

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        System.out.println("Booking Successful! Booking ID: " + keys.getInt(1));
                    } else {
                        System.out.println("Booking Successful!");
                    }
                }
            } catch (SQLException e) {
                System.out.println("Booking failed due to database error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter numbers correctly.");
        }
    }

    @Override
    public void viewBookings() {
        String sql = "SELECT booking_id, user_name, train_name FROM bookings ORDER BY booking_id";

        try (Connection connection = databaseService.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            boolean hasAnyBooking = false;
            while (resultSet.next()) {
                hasAnyBooking = true;
                System.out.println("-------------------");
                System.out.println("Booking ID: " + resultSet.getInt("booking_id"));
                System.out.println("User: " + resultSet.getString("user_name"));
                System.out.println("Train: " + resultSet.getString("train_name"));
            }

            if (!hasAnyBooking) {
                System.out.println("No bookings found.");
            }
        } catch (SQLException e) {
            System.out.println("Unable to fetch bookings: " + e.getMessage());
        }
    }
}