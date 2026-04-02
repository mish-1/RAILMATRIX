package service.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TrainDao {

    public String findTrainNameById(Connection connection, int trainId) throws SQLException {
        String sql = "SELECT train_name FROM `Train` WHERE train_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, trainId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("train_name");
                }
            }
        }

        return null;
    }

    public RouteEndpoints findRouteEndpoints(Connection connection, int trainId) throws SQLException {
        String sql = "SELECT s.station_id, s.station_name "
                + "FROM `Route` r "
                + "JOIN `Station` s ON s.station_id = r.station_id "
                + "WHERE r.train_id = ? "
                + "ORDER BY r.stop_number";

        RouteEndpoints endpoints = new RouteEndpoints();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, trainId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (endpoints.sourceStationId == -1) {
                        endpoints.sourceStationId = resultSet.getInt("station_id");
                        endpoints.sourceStationName = resultSet.getString("station_name");
                    }

                    endpoints.destinationStationId = resultSet.getInt("station_id");
                    endpoints.destinationStationName = resultSet.getString("station_name");
                }
            }
        }

        return endpoints;
    }

    public static class RouteEndpoints {
        public int sourceStationId = -1;
        public int destinationStationId = -1;
        public String sourceStationName = "Unknown";
        public String destinationStationName = "Unknown";

        public boolean isComplete() {
            return sourceStationId > 0 && destinationStationId > 0;
        }
    }
}
