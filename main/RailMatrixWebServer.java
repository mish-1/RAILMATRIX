package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import service.DatabaseService;
import service.dao.BookingDao;
import service.dao.TrainDao;
import service.dao.UserDao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RailMatrixWebServer {

    private static final int PORT = 8080;
    private static final String APP_NAME = "RailMatrix";
    private static final int MIN_SEATS_PER_BOOKING = 1;
    private static final int MAX_SEATS_PER_BOOKING = 6;
    private static final int MAX_SEATS_PER_TRAIN_PER_DAY = 120;

    private final DatabaseService databaseService;
    private final UserDao userDao;
    private final TrainDao trainDao;
    private final BookingDao bookingDao;

    public RailMatrixWebServer() {
        this.databaseService = new DatabaseService();
        this.userDao = new UserDao();
        this.trainDao = new TrainDao();
        this.bookingDao = new BookingDao();
        this.databaseService.initializeTrainSearchSchema();
        this.databaseService.initializeSchema();
    }

    public static void main(String[] args) throws IOException {
        RailMatrixWebServer app = new RailMatrixWebServer();
        app.start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new StaticPageHandler());
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/meta", new MetaHandler());
        server.createContext("/api/trains", new TrainsHandler());
        server.createContext("/api/bookings", new BookingsHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("RailMatrix web server running at http://localhost:" + PORT);
    }

    private class StaticPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (!"/".equals(path) && !"/output-viewer.html".equals(path)) {
                sendJson(exchange, 404, "{\"error\":\"Not found\"}");
                return;
            }

            java.nio.file.Path htmlPath = java.nio.file.Paths.get("output-viewer.html");
            if (!java.nio.file.Files.exists(htmlPath)) {
                sendJson(exchange, 404, "{\"error\":\"output-viewer.html not found\"}");
                return;
            }

            byte[] bytes = java.nio.file.Files.readAllBytes(htmlPath);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        }
    }

    private class MetaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String json = "{" +
                    "\"appName\":\"" + APP_NAME + "\"," +
                    "\"serverDate\":\"" + LocalDate.now() + "\"," +
                    "\"bookingRules\":{" +
                    "\"minSeatsPerBooking\":" + MIN_SEATS_PER_BOOKING + "," +
                    "\"maxSeatsPerBooking\":" + MAX_SEATS_PER_BOOKING + "," +
                    "\"maxSeatsPerTrainPerDay\":" + MAX_SEATS_PER_TRAIN_PER_DAY +
                    "}" +
                    "}";

            sendJson(exchange, 200, json);
        }
    }

    private class TrainsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            URI uri = exchange.getRequestURI();
            Map<String, String> queryParams = parseQuery(uri.getRawQuery());

            String source = safeTrim(queryParams.get("source"));
            String destination = safeTrim(queryParams.get("destination"));

            if (source.isEmpty() || destination.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"source and destination are required\"}");
                return;
            }

            String directQuery =
                    "SELECT DISTINCT t.train_id, t.train_name " +
                    "FROM `Route` r1 " +
                    "JOIN `Route` r2 ON r1.train_id = r2.train_id " +
                    "JOIN `Train` t ON t.train_id = r1.train_id " +
                    "JOIN `Station` s1 ON r1.station_id = s1.station_id " +
                    "JOIN `Station` s2 ON r2.station_id = s2.station_id " +
                    "WHERE LOWER(TRIM(s1.station_name)) LIKE ? " +
                    "AND LOWER(TRIM(s2.station_name)) LIKE ? " +
                    "AND r1.stop_number < r2.stop_number " +
                    "ORDER BY t.train_id";

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

            List<String> direct = new ArrayList<>();
            List<String> connecting = new ArrayList<>();

            try (Connection con = databaseService.getConnection()) {
                try (PreparedStatement ps1 = con.prepareStatement(directQuery)) {
                    ps1.setString(1, source.toLowerCase() + "%");
                    ps1.setString(2, destination.toLowerCase() + "%");
                    try (ResultSet rs = ps1.executeQuery()) {
                        while (rs.next()) {
                            direct.add("{\"trainId\":" + rs.getInt("train_id") + ",\"trainName\":\"" + escapeJson(rs.getString("train_name")) + "\"}");
                        }
                    }
                }

                try (PreparedStatement ps2 = con.prepareStatement(connectQuery)) {
                    ps2.setString(1, source.toLowerCase() + "%");
                    ps2.setString(2, destination.toLowerCase() + "%");
                    try (ResultSet rs = ps2.executeQuery()) {
                        while (rs.next()) {
                            String firstTrain = escapeJson(rs.getString("first_train"));
                            String secondTrain = escapeJson(rs.getString("second_train"));
                            String junction = escapeJson(rs.getString("junction"));
                            connecting.add("{\"firstTrain\":\"" + firstTrain + "\",\"secondTrain\":\"" + secondTrain + "\",\"junction\":\"" + junction + "\"}");
                        }
                    }
                }

                String json = "{" +
                        "\"source\":\"" + escapeJson(source) + "\"," +
                        "\"destination\":\"" + escapeJson(destination) + "\"," +
                        "\"direct\":[" + String.join(",", direct) + "]," +
                        "\"connecting\":[" + String.join(",", connecting) + "]" +
                        "}";
                sendJson(exchange, 200, json);
            } catch (SQLException e) {
                sendJson(exchange, 500, "{\"error\":\"Database query failed\",\"details\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                handleGetBookings(exchange);
                return;
            }
            if ("POST".equalsIgnoreCase(method)) {
                handleCreateBooking(exchange);
                return;
            }
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }

        private void handleGetBookings(HttpExchange exchange) throws IOException {
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
            String userIdParam = safeTrim(queryParams.get("userId"));
            if (userIdParam.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"userId is required for privacy\"}");
                return;
            }
            List<String> rows = new ArrayList<>();

            try (Connection con = databaseService.getConnection()) {
                int userId = Integer.parseInt(userIdParam);
                if (userId <= 0) {
                    sendJson(exchange, 400, "{\"error\":\"userId must be a positive number\"}");
                    return;
                }
                List<BookingDao.BookingView> data = bookingDao.fetchBookingsByUserViaProcedure(con, userId);
                int totalBookingsByUser = bookingDao.fetchTotalUserBookingsUsingFunction(con, userId);

                for (BookingDao.BookingView rs : data) {
                    rows.add("{" +
                            "\"bookingId\":" + rs.bookingId + "," +
                            "\"userId\":" + rs.userId + "," +
                            "\"userName\":\"" + escapeJson(rs.userName) + "\"," +
                            "\"trainId\":" + rs.trainId + "," +
                            "\"trainName\":\"" + escapeJson(rs.trainName) + "\"," +
                            "\"source\":\"" + escapeJson(rs.source) + "\"," +
                            "\"destination\":\"" + escapeJson(rs.destination) + "\"," +
                            "\"journeyDate\":\"" + escapeJson(rs.journeyDate) + "\"," +
                            "\"bookingDate\":\"" + escapeJson(rs.bookingDate) + "\"," +
                            "\"seatCount\":" + rs.seatCount + "," +
                            "\"status\":\"" + escapeJson(rs.status) + "\"" +
                            "}");
                }

                sendJson(exchange, 200, "{\"totalBookings\":" + totalBookingsByUser + ",\"bookings\":[" + String.join(",", rows) + "]}");
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"Invalid userId value\"}");
            } catch (SQLException e) {
                if (isRoutineMissingError(e)) {
                    sendJson(exchange, 500, "{\"error\":\"Required database routine is missing. Run railmatrix.sql to create procedures/functions/triggers.\",\"details\":\"" + escapeJson(e.getMessage()) + "\"}");
                    return;
                }
                sendJson(exchange, 500, "{\"error\":\"Unable to fetch bookings\",\"details\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }

        private void handleCreateBooking(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange.getRequestBody());

            int userId = parseJsonInt(body, "userId");
            int trainId = parseJsonInt(body, "trainId");
            String userName = parseJsonString(body, "userName");
            String journeyDate = parseJsonString(body, "journeyDate");
            int seatCount = parseJsonInt(body, "seatCount");

            if (userId <= 0 || trainId <= 0) {
                sendJson(exchange, 400, "{\"error\":\"userId and trainId must be positive numbers\"}");
                return;
            }
            if (!isValidUserName(userName)) {
                sendJson(exchange, 400, "{\"error\":\"userName must contain only letters/spaces and be 2-50 characters\"}");
                return;
            }
            if (journeyDate.isBlank()) {
                journeyDate = java.time.LocalDate.now().toString();
            }
            if (!isValidJourneyDate(journeyDate)) {
                sendJson(exchange, 400, "{\"error\":\"journeyDate must be today or a future date in YYYY-MM-DD format\"}");
                return;
            }
            if (seatCount <= 0) {
                seatCount = MIN_SEATS_PER_BOOKING;
            }
            if (!isValidSeatCount(seatCount)) {
                sendJson(exchange, 400, "{\"error\":\"seatCount must be between " + MIN_SEATS_PER_BOOKING + " and " + MAX_SEATS_PER_BOOKING + "\"}");
                return;
            }

            try (Connection con = databaseService.getConnection()) {
                String trainName = trainDao.findTrainNameById(con, trainId);
                if (trainName == null) {
                    sendJson(exchange, 404, "{\"error\":\"Train not found\"}");
                    return;
                }

                TrainDao.RouteEndpoints endpoints = trainDao.findRouteEndpoints(con, trainId);
                if (!endpoints.isComplete()) {
                    sendJson(exchange, 400, "{\"error\":\"Unable to resolve train route stations\"}");
                    return;
                }

                int reservedSeats = bookingDao.fetchReservedSeatsForTrainAndDate(con, trainId, journeyDate);
                if (reservedSeats + seatCount > MAX_SEATS_PER_TRAIN_PER_DAY) {
                    int available = Math.max(0, MAX_SEATS_PER_TRAIN_PER_DAY - reservedSeats);
                    sendJson(exchange, 400, "{\"error\":\"Not enough seats available for this train on selected date\",\"availableSeats\":" + available + "}");
                    return;
                }

                userDao.upsertUser(con, userId, userName);

                int bookingId = bookingDao.createBooking(
                        con,
                        userId,
                        trainId,
                        endpoints.sourceStationId,
                        endpoints.destinationStationId,
                        journeyDate,
                        seatCount
                );

                int fare = bookingDao.fetchFareUsingFunction(con, seatCount);
                int totalBookingsByUser = bookingDao.fetchTotalUserBookingsUsingFunction(con, userId);

                String bookingDate = "";
                String bookingStatus = "";
                List<BookingDao.BookingView> viaProcedure = bookingDao.fetchBookingsByUserViaProcedure(con, userId);
                for (BookingDao.BookingView row : viaProcedure) {
                    if (row.bookingId == bookingId) {
                        bookingDate = safeTrim(row.bookingDate);
                        bookingStatus = safeTrim(row.status);
                        break;
                    }
                }

                if (bookingDate.isEmpty() || bookingStatus.isEmpty()) {
                    throw new SQLException("Created booking was not returned by view_user_bookings procedure.");
                }

                sendJson(exchange, 201, "{" +
                        "\"message\":\"Booking successful\"," +
                        "\"bookingId\":" + bookingId + "," +
                        "\"trainId\":" + trainId + "," +
                        "\"trainName\":\"" + escapeJson(trainName) + "\"," +
                        "\"source\":\"" + escapeJson(endpoints.sourceStationName) + "\"," +
                        "\"destination\":\"" + escapeJson(endpoints.destinationStationName) + "\"," +
                        "\"journeyDate\":\"" + escapeJson(journeyDate) + "\"," +
                        "\"seatCount\":" + seatCount + "," +
                        "\"fare\":" + fare + "," +
                        "\"bookingDate\":\"" + escapeJson(bookingDate) + "\"," +
                        "\"status\":\"" + escapeJson(bookingStatus) + "\"," +
                        "\"totalBookings\":" + totalBookingsByUser +
                        "}");
            } catch (SQLException e) {
                if (isRoutineMissingError(e)) {
                    sendJson(exchange, 500, "{\"error\":\"Required database routine is missing. Run railmatrix.sql to create procedures/functions/triggers.\",\"details\":\"" + escapeJson(e.getMessage()) + "\"}");
                    return;
                }
                sendJson(exchange, 500, "{\"error\":\"Booking failed\",\"details\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = decodeUrl(kv[0]);
            String value = kv.length > 1 ? decodeUrl(kv[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String decodeUrl(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isValidUserName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.matches("[A-Za-z ]{2,50}");
    }

    private static boolean isValidJourneyDate(String value) {
        try {
            LocalDate parsed = LocalDate.parse(value);
            return !parsed.isBefore(LocalDate.now());
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean isValidSeatCount(int seatCount) {
        return seatCount >= MIN_SEATS_PER_BOOKING && seatCount <= MAX_SEATS_PER_BOOKING;
    }

    private static String readRequestBody(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int parseJsonInt(String json, String key) {
        String raw = parseJsonString(json, key);
        if (raw.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String parseJsonString(String json, String key) {
        if (json == null || json.isBlank()) {
            return "";
        }

        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }

        int index = colonIndex + 1;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }

        if (index >= json.length()) {
            return "";
        }

        if (json.charAt(index) == '"') {
            int end = index + 1;
            StringBuilder value = new StringBuilder();
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\' && end + 1 < json.length()) {
                    char next = json.charAt(end + 1);
                    value.append(next);
                    end += 2;
                    continue;
                }
                if (c == '"') {
                    return value.toString();
                }
                value.append(c);
                end++;
            }
            return value.toString();
        }

        int end = index;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && !Character.isWhitespace(json.charAt(end))) {
            end++;
        }
        return json.substring(index, end);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static boolean isRoutineMissingError(SQLException e) {
        int code = e.getErrorCode();
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return code == 1305
                || (message.contains("does not exist") && (message.contains("procedure") || message.contains("function")));
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
