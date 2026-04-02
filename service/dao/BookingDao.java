package service.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookingDao {

    public int createBooking(
            Connection connection,
            int userId,
            int trainId,
            int sourceStationId,
            int destinationStationId,
            String journeyDate,
            int seatCount
    ) throws SQLException {
        String sql = "INSERT INTO `Booking` (user_id, train_id, source_station_id, destination_station_id, journey_date, booking_date, seat_count, booking_status) "
                + "VALUES (?, ?, ?, ?, ?, CURDATE(), ?, 'Confirmed')";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setInt(2, trainId);
            statement.setInt(3, sourceStationId);
            statement.setInt(4, destinationStationId);
            statement.setString(5, journeyDate);
            statement.setInt(6, seatCount);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return -1;
    }

    public List<BookingView> fetchAllBookings(Connection connection) throws SQLException {
        String sql = "SELECT b.booking_id, b.user_id, u.user_name, b.train_id, t.train_name, "
                + "s1.station_name AS source, s2.station_name AS destination, "
                + "b.journey_date, b.seat_count, b.booking_status "
                + "FROM `Booking` b "
                + "JOIN `User` u ON u.user_id = b.user_id "
                + "JOIN `Train` t ON t.train_id = b.train_id "
                + "JOIN `Station` s1 ON s1.station_id = b.source_station_id "
                + "JOIN `Station` s2 ON s2.station_id = b.destination_station_id "
                + "ORDER BY b.booking_id";

        List<BookingView> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rows.add(mapBookingView(resultSet));
            }
        }

        return rows;
    }

    public List<BookingView> fetchBookingsByUserId(Connection connection, int userId) throws SQLException {
        String sql = "SELECT b.booking_id, b.user_id, u.user_name, b.train_id, t.train_name, "
                + "s1.station_name AS source, s2.station_name AS destination, "
                + "b.journey_date, b.seat_count, b.booking_status "
                + "FROM `Booking` b "
                + "JOIN `User` u ON u.user_id = b.user_id "
                + "JOIN `Train` t ON t.train_id = b.train_id "
                + "JOIN `Station` s1 ON s1.station_id = b.source_station_id "
                + "JOIN `Station` s2 ON s2.station_id = b.destination_station_id "
                + "WHERE b.user_id = ? "
                + "ORDER BY b.booking_id";

        List<BookingView> rows = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapBookingView(resultSet));
                }
            }
        }

        return rows;
    }

    public int fetchReservedSeatsForTrainAndDate(Connection connection, int trainId, String journeyDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(seat_count), 0) AS reserved "
                + "FROM `Booking` "
                + "WHERE train_id = ? AND journey_date = ? AND booking_status IN ('Confirmed', 'Pending')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, trainId);
            statement.setString(2, journeyDate);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("reserved");
                }
            }
        }

        return 0;
    }

    private BookingView mapBookingView(ResultSet resultSet) throws SQLException {
        BookingView row = new BookingView();
        row.bookingId = resultSet.getInt("booking_id");
        row.userId = resultSet.getInt("user_id");
        row.userName = resultSet.getString("user_name");
        row.trainId = resultSet.getInt("train_id");
        row.trainName = resultSet.getString("train_name");
        row.source = resultSet.getString("source");
        row.destination = resultSet.getString("destination");
        row.journeyDate = resultSet.getString("journey_date");
        row.seatCount = resultSet.getInt("seat_count");
        row.status = resultSet.getString("booking_status");
        return row;
    }

    public static class BookingView {
        public int bookingId;
        public int userId;
        public String userName;
        public int trainId;
        public String trainName;
        public String source;
        public String destination;
        public String journeyDate;
        public int seatCount;
        public String status;
    }
}
