package service.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDao {

    public void upsertUser(Connection connection, int userId, String userName) throws SQLException {
        String sql = "INSERT INTO `User` (user_id, user_name, email, phone_number) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE user_name = VALUES(user_name)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, userName.trim());
            statement.setString(3, "user" + userId + "@railmatrix.local");
            statement.setString(4, "0000000000");
            statement.executeUpdate();
        }
    }
}
