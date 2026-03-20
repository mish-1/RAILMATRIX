-- ===============================
-- 1. CREATE DATABASE
-- ===============================
CREATE DATABASE RailMatrix;
USE RailMatrix;

-- ===============================
-- 2. USER TABLE
-- ===============================
CREATE TABLE USER (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone_number VARCHAR(15) UNIQUE
);

-- ===============================
-- 3. TRAIN TABLE
-- ===============================
CREATE TABLE TRAIN (
    train_id INT PRIMARY KEY AUTO_INCREMENT,
    train_number VARCHAR(20) UNIQUE NOT NULL,
    train_name VARCHAR(100) NOT NULL,
    train_type VARCHAR(50),
    days_of_run VARCHAR(50)
);

-- ===============================
-- 4. STATION TABLE
-- ===============================
CREATE TABLE STATION (
    station_id INT PRIMARY KEY AUTO_INCREMENT,
    station_code VARCHAR(10) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    state VARCHAR(50),
    zone VARCHAR(50),
    station_type ENUM('Junction','Terminal','Halt')
);

-- ===============================
-- 5. ROUTE TABLE
-- ===============================
CREATE TABLE ROUTE (
    route_id INT PRIMARY KEY AUTO_INCREMENT,
    train_id INT,
    station_id INT,
    stop_number INT,
    arrival_time TIME,
    departure_time TIME,
    halt_duration INT,
    distance_from_source INT,

    FOREIGN KEY (train_id) REFERENCES TRAIN(train_id),
    FOREIGN KEY (station_id) REFERENCES STATION(station_id)
);

-- ===============================
-- 6. BOOKING TABLE
-- ===============================
CREATE TABLE BOOKING (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    train_id INT,
    source_station_id INT,
    destination_station_id INT,
    journey_date DATE,
    booking_date DATE,
    seat_count INT,
    booking_status VARCHAR(20),

    FOREIGN KEY (user_id) REFERENCES USER(user_id),
    FOREIGN KEY (train_id) REFERENCES TRAIN(train_id),
    FOREIGN KEY (source_station_id) REFERENCES STATION(station_id),
    FOREIGN KEY (destination_station_id) REFERENCES STATION(station_id)
);

-- ===============================
-- 7. STOP TABLE
-- ===============================
CREATE TABLE STOP (
    stop_id INT PRIMARY KEY AUTO_INCREMENT,
    route_id INT,
    station_id INT,
    stop_sequence INT,
    arrival_time TIME,
    departure_time TIME,
    halt_duration INT,

    FOREIGN KEY (route_id) REFERENCES ROUTE(route_id),
    FOREIGN KEY (station_id) REFERENCES STATION(station_id)
);

-- ===============================
-- 8. TICKET TABLE
-- ===============================
CREATE TABLE TICKET (
    ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT,
    passenger_name VARCHAR(100),
    coach_no VARCHAR(10),
    seat_no VARCHAR(10),
    fare DECIMAL(10,2),

    FOREIGN KEY (booking_id) REFERENCES BOOKING(booking_id)
);

-- ===============================
-- 9. SAMPLE DATA INSERTION
-- ===============================

-- Stations
INSERT INTO STATION (station_code, station_name, state, zone, station_type)
VALUES 
('PUNE','Pune Junction','Maharashtra','Central','Junction'),
('MUM','Mumbai Central','Maharashtra','Western','Terminal'),
('DEL','New Delhi','Delhi','Northern','Junction'),
('JAI','Jaipur','Rajasthan','North Western','Junction');

-- Trains
INSERT INTO TRAIN (train_number, train_name, train_type, days_of_run)
VALUES 
('12123','Deccan Express','Superfast','Mon,Tue,Wed'),
('12951','Rajdhani Express','Premium','Daily');

-- Routes
INSERT INTO ROUTE (train_id, station_id, stop_number, arrival_time, departure_time, halt_duration, distance_from_source)
VALUES
(1,1,1,'08:00:00','08:05:00',5,0),
(1,2,2,'12:00:00','12:10:00',10,150),
(2,3,1,'06:00:00','06:10:00',10,0),
(2,4,2,'10:00:00','10:05:00',5,250);

-- Users
INSERT INTO USER (user_name, email, phone_number)
VALUES 
('Mishti','mishti@gmail.com','9876543210');

-- Booking
INSERT INTO BOOKING (user_id, train_id, source_station_id, destination_station_id, journey_date, booking_date, seat_count, booking_status)
VALUES 
(1,1,1,2,'2026-03-20',CURDATE(),2,'Confirmed');

-- Ticket
INSERT INTO TICKET (booking_id, passenger_name, coach_no, seat_no, fare)
VALUES
(1,'Mishti Kinker','S1','23',500.00);

-- ===============================
-- 10. IMPORTANT QUERIES
-- ===============================

-- View all trains
SELECT * FROM TRAIN;

-- Direct Train Search
SELECT t.train_name
FROM ROUTE r1
JOIN ROUTE r2 ON r1.train_id = r2.train_id
JOIN TRAIN t ON t.train_id = r1.train_id
WHERE r1.station_id = 1
AND r2.station_id = 2
AND r1.stop_number < r2.stop_number;

-- Connecting Trains
SELECT 
    t1.train_name AS First_Train,
    t2.train_name AS Second_Train,
    s.station_name AS Junction
FROM ROUTE r1
JOIN ROUTE r2 ON r1.station_id = r2.station_id
JOIN TRAIN t1 ON r1.train_id = t1.train_id
JOIN TRAIN t2 ON r2.train_id = t2.train_id
JOIN STATION s ON s.station_id = r1.station_id
WHERE r1.departure_time < r2.arrival_time
AND r1.train_id <> r2.train_id;

-- View bookings
SELECT b.booking_id, u.user_name, t.train_name, b.booking_status
FROM BOOKING b
JOIN USER u ON b.user_id = u.user_id
JOIN TRAIN t ON b.train_id = t.train_id;
