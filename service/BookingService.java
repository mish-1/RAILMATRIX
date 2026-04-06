package service;

import model.*;
import service.dao.BookingDao;
import service.dao.TrainDao;
import service.dao.UserDao;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;

public class BookingService implements BookingOperations {

    private static final int MAX_SEATS_PER_TRAIN_PER_DAY = 120;

    private final TrainService trainService;
    private final Scanner scanner;
    private final DatabaseService databaseService;
    private final UserDao userDao;
    private final TrainDao trainDao;
    private final BookingDao bookingDao;

    public BookingService(TrainService trainService, Scanner scanner) {
        this.trainService = trainService;
        this.scanner = scanner;
        this.databaseService = new DatabaseService();
        this.userDao = new UserDao();
        this.trainDao = new TrainDao();
        this.bookingDao = new BookingDao();
        this.databaseService.initializeSchema();
    }

    @Override
    public void bookTicket() {

        try {
            System.out.print("Enter User ID: ");
            int userId = Integer.parseInt(scanner.nextLine());
            if (userId <= 0) {
                System.out.println("User ID must be a positive number.");
                return;
            }

            System.out.print("Enter User Name: ");
            String name = scanner.nextLine();
            if (!isValidUserName(name)) {
                System.out.println("User name must contain only letters/spaces and be at least 2 characters.");
                return;
            }

            User user = new User(userId, name);

            System.out.print("Enter Train ID to book: ");
            int trainId = Integer.parseInt(scanner.nextLine());

            Train train = trainService.getTrainById(trainId);

            if (train == null) {
                System.out.println("Invalid Train ID!");
                return;
            }

            System.out.print("Enter Journey Date (YYYY-MM-DD): ");
            String journeyDate = scanner.nextLine().trim();
            if (!isValidJourneyDate(journeyDate)) {
                System.out.println("Journey date must be today or a future date in YYYY-MM-DD format.");
                return;
            }

            System.out.print("Enter Seat Count: ");
            int seatCount = Integer.parseInt(scanner.nextLine());
            if (!isValidSeatCount(seatCount)) {
                System.out.println("Seat count must be between 1 and 6.");
                return;
            }

            try (Connection connection = databaseService.getConnection()) {
                userDao.upsertUser(connection, user.getUserId(), user.getName());

                TrainDao.RouteEndpoints endpoints = trainDao.findRouteEndpoints(connection, train.getTrainId());
                if (!endpoints.isComplete()) {
                    System.out.println("Unable to resolve source/destination stations for selected train.");
                    return;
                }

                int reservedSeats = bookingDao.fetchReservedSeatsForTrainAndDate(connection, train.getTrainId(), journeyDate);
                if (reservedSeats + seatCount > MAX_SEATS_PER_TRAIN_PER_DAY) {
                    int available = Math.max(0, MAX_SEATS_PER_TRAIN_PER_DAY - reservedSeats);
                    System.out.println("Booking failed. Only " + available + " seat(s) are available for this train on " + journeyDate + ".");
                    return;
                }

                int bookingId = bookingDao.createBooking(
                        connection,
                        user.getUserId(),
                        train.getTrainId(),
                        endpoints.sourceStationId,
                        endpoints.destinationStationId,
                        journeyDate,
                        seatCount
                );

                int fare = bookingDao.fetchFareUsingFunction(connection, seatCount);
                int totalBookingsByUser = bookingDao.fetchTotalUserBookingsUsingFunction(connection, user.getUserId());

                BookingDao.BookingView insertedRow = null;
                java.util.List<BookingDao.BookingView> rows = bookingDao.fetchBookingsByUserViaProcedure(connection, user.getUserId());
                for (BookingDao.BookingView row : rows) {
                    if (row.bookingId == bookingId) {
                        insertedRow = row;
                        break;
                    }
                }

                if (bookingId > 0) {
                    System.out.println("Booking Successful! Booking ID: " + bookingId
                            + " | Train ID: " + train.getTrainId());
                } else {
                    System.out.println("Booking Successful!");
                }

                System.out.println("Fare: " + fare);
                if (insertedRow == null) {
                    throw new SQLException("Created booking was not returned by view_user_bookings procedure.");
                }
                System.out.println("Booking Date: " + insertedRow.bookingDate);
                System.out.println("Status: " + insertedRow.status);
                System.out.println("Total bookings by this user: " + totalBookingsByUser);
            } catch (SQLException e) {
                System.out.println("Booking failed due to database error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter numbers correctly.");
        }
    }

    @Override
    public void viewBookings() {
        System.out.print("Enter User ID to view bookings: ");

        int userId;
        try {
            userId = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException ex) {
            System.out.println("Invalid User ID.");
            return;
        }

        if (userId <= 0) {
            System.out.println("User ID must be a positive number.");
            return;
        }

        try (Connection connection = databaseService.getConnection()) {
            java.util.List<BookingDao.BookingView> rows = bookingDao.fetchBookingsByUserViaProcedure(connection, userId);
            int totalBookingsByUser = bookingDao.fetchTotalUserBookingsUsingFunction(connection, userId);

            boolean hasAnyBooking = false;
            for (BookingDao.BookingView row : rows) {
                hasAnyBooking = true;
                System.out.println("-------------------");
                System.out.println("Booking ID: " + row.bookingId);
                System.out.println("User: " + row.userName);
                if (row.trainId > 0) {
                    System.out.println("Train: " + row.trainName + " (ID " + row.trainId + ")");
                } else {
                    System.out.println("Train: " + row.trainName);
                }
                System.out.println("Route: " + row.source + " -> " + row.destination);
                System.out.println("Journey Date: " + row.journeyDate);
                if (row.bookingDate != null && !row.bookingDate.isBlank()) {
                    System.out.println("Booking Date: " + row.bookingDate);
                }
                System.out.println("Seat Count: " + row.seatCount);
                System.out.println("Status: " + row.status);
            }

            if (!hasAnyBooking) {
                System.out.println("No bookings found.");
            } else {
                System.out.println("\nTotal bookings by this user: " + totalBookingsByUser);
            }
        } catch (SQLException e) {
            System.out.println("Unable to fetch bookings: " + e.getMessage());
        }
    }

    private boolean isValidUserName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.matches("[A-Za-z ]{2,50}");
    }

    private boolean isValidJourneyDate(String dateValue) {
        try {
            LocalDate parsed = LocalDate.parse(dateValue);
            return !parsed.isBefore(LocalDate.now());
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean isValidSeatCount(int seatCount) {
        return seatCount >= 1 && seatCount <= 6;
    }
}