package main;

import service.*;
import java.util.Scanner;

public class RailMatrixApp {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        TrainService trainService = new TrainService(scanner);
        BookingService bookingService = new BookingService(trainService, scanner);

        while (true) {

            System.out.print("\nEnter Source Station: ");
            String source = scanner.nextLine().trim();

            System.out.print("Enter Destination Station: ");
            String destination = scanner.nextLine().trim();

            if (source.isEmpty() || destination.isEmpty()) {
                System.out.println("Source and destination cannot be empty. Please try again.");
                continue;
            }

            trainService.searchTrains(source, destination);

            System.out.println("\nWhat would you like to do?");
            System.out.println("1. Book a Ticket");
            System.out.println("2. View My Bookings");
            System.out.println("3. Search Again");
            System.out.println("4. Exit");
            System.out.print("Choose option: ");

            if (!scanner.hasNextInt()) {
                scanner.nextLine();
                continue;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    bookingService.bookTicket();
                    break;
                case 2:
                    bookingService.viewBookings();
                    break;
                case 3:
                    break;
                case 4:
                    System.out.println("Exiting...");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
}