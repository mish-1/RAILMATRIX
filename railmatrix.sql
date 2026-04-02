CREATE DATABASE IF NOT EXISTS ConnectingTrainDB;
USE ConnectingTrainDB;

CREATE TABLE IF NOT EXISTS `User` (
    user_id INT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS `Train` (
    train_id INT PRIMARY KEY,
    train_number INT UNIQUE NOT NULL,
    train_name VARCHAR(100) NOT NULL,
    train_type VARCHAR(30),
    days_of_run VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS `Station` (
    station_id INT PRIMARY KEY,
    station_code VARCHAR(10) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    state VARCHAR(50),
    zone VARCHAR(20),
    station_type VARCHAR(20),
    CHECK (station_type IN ('Junction', 'Terminal', 'Halt'))
);

CREATE TABLE IF NOT EXISTS `Route` (
    route_id INT PRIMARY KEY,
    train_id INT NOT NULL,
    station_id INT NOT NULL,
    stop_number INT NOT NULL,
    arrival_time TIME,
    departure_time TIME,
    halt_duration INT,
    distance_from_source INT,
    FOREIGN KEY (train_id) REFERENCES `Train`(train_id),
    FOREIGN KEY (station_id) REFERENCES `Station`(station_id)
);

CREATE TABLE IF NOT EXISTS `Stop` (
    stop_id INT AUTO_INCREMENT PRIMARY KEY,
    route_id INT NOT NULL,
    station_id INT NOT NULL,
    stop_sequence INT NOT NULL,
    arrival_time TIME,
    departure_time TIME,
    halt_duration INT,
    FOREIGN KEY (route_id) REFERENCES `Route`(route_id),
    FOREIGN KEY (station_id) REFERENCES `Station`(station_id)
);

CREATE TABLE IF NOT EXISTS `Booking` (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    train_id INT NOT NULL,
    source_station_id INT NOT NULL,
    destination_station_id INT NOT NULL,
    journey_date DATE NOT NULL,
    booking_date DATE NOT NULL,
    seat_count INT NOT NULL,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'Confirmed',
    CHECK (booking_status IN ('Confirmed', 'Pending', 'Cancelled')),
    FOREIGN KEY (user_id) REFERENCES `User`(user_id),
    FOREIGN KEY (train_id) REFERENCES `Train`(train_id),
    FOREIGN KEY (source_station_id) REFERENCES `Station`(station_id),
    FOREIGN KEY (destination_station_id) REFERENCES `Station`(station_id)
);

CREATE TABLE IF NOT EXISTS `Ticket` (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    coach_no VARCHAR(10),
    seat_no VARCHAR(10),
    fare DECIMAL(8,2),
    FOREIGN KEY (booking_id) REFERENCES `Booking`(booking_id)
);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 901, 'R901', 'Mumbai', 'Maharashtra', 'WR', 'Terminal' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 901);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 902, 'R902', 'Lonavala', 'Maharashtra', 'CR', 'Junction' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 902);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 903, 'R903', 'Pune', 'Maharashtra', 'CR', 'Junction' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 903);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 904, 'R904', 'Bangalore', 'Karnataka', 'SWR', 'Junction' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 904);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 905, 'R905', 'Chennai', 'Tamil Nadu', 'SR', 'Terminal' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 905);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 906, 'R906', 'Delhi', 'Delhi', 'NR', 'Terminal' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 906);

INSERT INTO `Station` (station_id, station_code, station_name, state, zone, station_type)
SELECT 907, 'R907', 'Jaipur', 'Rajasthan', 'NWR', 'Junction' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Station` WHERE station_id = 907);

INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run)
SELECT 901, 12001, 'RedLine Express', 'Express', 'Daily' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 901);

INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run)
SELECT 902, 12002, 'Night Rider', 'Superfast', 'Daily' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 902);

INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run)
SELECT 903, 12003, 'Coastal Runner', 'Express', 'Daily' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 903);

INSERT INTO `Train` (train_id, train_number, train_name, train_type, days_of_run)
SELECT 904, 12004, 'Western Link', 'Intercity', 'Daily' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Train` WHERE train_id = 904);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9001, 901, 901, 1, NULL, '06:00:00', 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9001);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9002, 901, 902, 2, '08:00:00', '08:05:00', 5, 110 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9002);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9003, 901, 903, 3, '09:30:00', NULL, 0, 180 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9003);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9004, 902, 906, 1, NULL, '22:00:00', 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9004);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9005, 902, 907, 2, '03:00:00', NULL, 0, 280 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9005);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9006, 903, 903, 1, NULL, '10:00:00', 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9006);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9007, 903, 904, 2, '18:00:00', '18:10:00', 10, 840 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9007);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9008, 903, 905, 3, '23:00:00', NULL, 0, 1200 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9008);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9009, 904, 901, 1, NULL, '07:30:00', 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9009);

INSERT INTO `Route` (route_id, train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
SELECT 9010, 904, 904, 2, '22:00:00', NULL, 0, 980 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `Route` WHERE route_id = 9010);

INSERT INTO `Stop` (route_id, station_id, stop_sequence, arrival_time, departure_time, halt_duration)
SELECT r.route_id, r.station_id, r.stop_number, r.arrival_time, r.departure_time,
       CASE WHEN r.arrival_time IS NOT NULL AND r.departure_time IS NOT NULL
            THEN TIMESTAMPDIFF(MINUTE, r.arrival_time, r.departure_time)
            ELSE NULL END
FROM `Route` r
WHERE NOT EXISTS (
    SELECT 1 FROM `Stop` s
    WHERE s.route_id = r.route_id AND s.stop_sequence = r.stop_number
);

SELECT t.train_id, t.train_number, t.train_name FROM `Train` t ORDER BY t.train_id;
