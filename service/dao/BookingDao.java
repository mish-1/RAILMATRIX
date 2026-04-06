package service.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        int bookingId = createBookingUsingProcedure(
                connection,
                userId,
                trainId,
                sourceStationId,
                destinationStationId,
                journeyDate,
                seatCount
        );

        if (bookingId <= 0) {
            throw new SQLException("Booking creation failed via add_booking procedure.");
        }

        return bookingId;
    }

    private int createBookingUsingProcedure(
            Connection connection,
            int userId,
            int trainId,
            int sourceStationId,
            int destinationStationId,
            String journeyDate,
            int seatCount
    ) throws SQLException {
        String call = "{CALL add_booking(?, ?, ?, ?, ?, ?)}";

        try (CallableStatement statement = connection.prepareCall(call)) {
            statement.setInt(1, userId);
            statement.setInt(2, trainId);
            statement.setInt(3, sourceStationId);
            statement.setInt(4, destinationStationId);
            statement.setInt(5, seatCount);
            statement.setDate(6, Date.valueOf(journeyDate));
            statement.execute();
        }

        try (PreparedStatement keyStatement = connection.prepareStatement("SELECT LAST_INSERT_ID() AS booking_id");
             ResultSet keys = keyStatement.executeQuery()) {
            if (keys.next()) {
                return keys.getInt("booking_id");
            }
        }

        throw new SQLException("Unable to fetch booking_id after add_booking procedure call.");
    }

    public int fetchFareUsingFunction(Connection connection, int seatCount) throws SQLException {
        String sql = "SELECT calculate_fare(?) AS fare";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, seatCount);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("fare");
                }
            }
        }

        throw new SQLException("calculate_fare function returned no result.");
    }

    public int fetchTotalUserBookingsUsingFunction(Connection connection, int userId) throws SQLException {
        String sql = "SELECT total_user_bookings(?) AS total";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
            }
        }

        throw new SQLException("total_user_bookings function returned no result.");
    }

    public List<BookingView> fetchBookingsByUserViaProcedure(Connection connection, int userId) throws SQLException {
        String call = "{CALL view_user_bookings(?)}";
        List<BookingView> rows = new ArrayList<>();

        try (CallableStatement statement = connection.prepareCall(call)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BookingView row = new BookingView();
                    row.bookingId = resultSet.getInt("booking_id");
                    row.userId = userId;
                    row.userName = resultSet.getString("user_name");
                    row.trainId = -1;
                    row.trainName = resultSet.getString("train_name");
                    row.source = resultSet.getString("source");
                    row.destination = resultSet.getString("destination");
                    row.journeyDate = resultSet.getString("journey_date");
                    row.bookingDate = resultSet.getString("booking_date");
                    row.seatCount = resultSet.getInt("seat_count");
                    row.status = resultSet.getString("booking_status");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    public List<BookingView> fetchAllBookings(Connection connection) throws SQLException {
        String sql = "SELECT b.booking_id, b.user_id, u.user_name, b.train_id, t.train_name, "
                + "s1.station_name AS source, s2.station_name AS destination, "
                + "b.journey_date, b.booking_date, b.seat_count, b.booking_status "
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
                + "b.journey_date, b.booking_date, b.seat_count, b.booking_status "
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
        row.bookingDate = resultSet.getString("booking_date");
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
        public String bookingDate;
        public int seatCount;
        public String status;
    }
}
