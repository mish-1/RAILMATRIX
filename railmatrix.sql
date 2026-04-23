show databases;
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

-- Function 1 to calculate fare
DELIMITER $$

CREATE FUNCTION calculate_fare(seats INT)
RETURNS INT
DETERMINISTIC
BEGIN
    RETURN seats * 150;
END$$

DELIMITER ;
SELECT calculate_fare(3);

-- Function 2 to calculate total number of bookings by user
DELIMITER $$

CREATE FUNCTION total_user_bookings(uid INT)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE total INT;

    SELECT COUNT(*) INTO total
    FROM Booking
    WHERE user_id = uid;

    RETURN total;
END$$

DELIMITER ;
SELECT total_user_bookings(1);

-- Procedure 1 to add bookings
DROP PROCEDURE IF EXISTS add_booking;
DELIMITER $$

CREATE PROCEDURE add_booking(
    IN uid INT,
    IN tid INT,
    IN src INT,
    IN dest INT,
    IN seats INT,
    IN jdate DATE
)
BEGIN
    INSERT INTO Booking(
        user_id,
        train_id,
        source_station_id,
        destination_station_id,
        journey_date,
        seat_count
    )
    VALUES(
        uid,
        tid,
        src,
        dest,
        jdate,
        seats
    );
END$$

DELIMITER;

CALL add_booking(1, 901, 901, 903, 2, '2026-05-01');
CALL add_booking(2, 901, 901, 903, 2, '2026-05-01');
CALL add_booking(2, 901, 901, 903, 2, '2026-05-01');

-- Procedure 2 to view user bookings

DROP PROCEDURE IF EXISTS view_user_bookings;

DELIMITER $$

CREATE PROCEDURE view_user_bookings(IN uid INT)
BEGIN
    SELECT 
        b.booking_id,
        u.user_name,
        t.train_name,
        s1.station_name AS source,
        s2.station_name AS destination,
        b.journey_date,
        b.booking_date,
        b.seat_count,
        b.booking_status
    FROM Booking b
    JOIN User u ON b.user_id = u.user_id
    JOIN Train t ON b.train_id = t.train_id
    JOIN Station s1 ON b.source_station_id = s1.station_id
    JOIN Station s2 ON b.destination_station_id = s2.station_id
    WHERE b.user_id = uid;
END$$

DELIMITER ;
CALL view_user_bookings(2);

-- Procedure 3 to update booking
DROP PROCEDURE IF EXISTS update_booking;
DELIMITER $$

CREATE PROCEDURE update_booking(
    IN bid INT,
    IN uid INT,
    IN seats INT,
    IN jdate DATE,
    IN bstatus VARCHAR(20),
    OUT rows_updated INT
)
BEGIN
    IF seats < 1 OR seats > 6 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Seat count must be between 1 and 6';
    END IF;

    IF jdate < CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Journey date must be today or a future date';
    END IF;

    IF bstatus NOT IN ('Confirmed', 'Pending', 'Cancelled') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid booking status';
    END IF;

    UPDATE Booking
    SET seat_count = seats,
        journey_date = jdate,
        booking_status = bstatus
    WHERE booking_id = bid
      AND user_id = uid;

    SET rows_updated = ROW_COUNT();
END$$

DELIMITER ;

-- Procedure 4 to delete booking
DROP PROCEDURE IF EXISTS delete_booking;
DELIMITER $$

CREATE PROCEDURE delete_booking(
    IN bid INT,
    IN uid INT,
    OUT rows_deleted INT
)
BEGIN
        DECLARE journey_dt DATETIME;

        SET rows_deleted = 0;

        SELECT TIMESTAMP(journey_date, '00:00:00')
        INTO journey_dt
        FROM Booking
        WHERE booking_id = bid
            AND user_id = uid
        LIMIT 1;

        IF journey_dt IS NULL THEN
                SET rows_deleted = 0;
        ELSEIF TIMESTAMPDIFF(HOUR, NOW(), journey_dt) < 48 THEN
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Booking can only be deleted at least 48 hours before journey date';
        ELSE
                DELETE FROM Ticket
                WHERE booking_id = bid;

                DELETE FROM Booking
                WHERE booking_id = bid
                    AND user_id = uid;

                SET rows_deleted = ROW_COUNT();
        END IF;
END$$

DELIMITER ;

-- Trigger 1 to check seat limit
DELIMITER $$

CREATE TRIGGER check_seat_limit
BEFORE INSERT ON Booking
FOR EACH ROW
BEGIN
    IF NEW.seat_count > 6 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Max 6 seats allowed';
    END IF;
END$$

DELIMITER ;

-- Trigger 2 to set auto booking date
DELIMITER $$

CREATE TRIGGER set_booking_date
BEFORE INSERT ON Booking
FOR EACH ROW
BEGIN
    SET NEW.booking_date = CURDATE();
END$$

DELIMITER ;

-- Trigger 3 to add ticket
DELIMITER $$

CREATE TRIGGER create_ticket_after_booking
AFTER INSERT ON Booking
FOR EACH ROW
BEGIN
    DECLARE uname VARCHAR(100);

    -- Get user name from User table
    SELECT user_name INTO uname
    FROM User
    WHERE user_id = NEW.user_id;

    -- Insert into Ticket using that name
    INSERT INTO Ticket (booking_id, passenger_name, coach_no, seat_no, fare)
    VALUES (NEW.booking_id, uname, 'S1', '1', 200);
END $$

DELIMITER ;
select * from booking;

SELECT DISTINCT t.train_id, t.train_name
FROM ROUTE r1
JOIN ROUTE r2 ON r1.train_id = r2.train_id
JOIN TRAIN t ON t.train_id = r1.train_id
JOIN STATION s1 ON r1.station_id = s1.station_id
JOIN STATION s2 ON r2.station_id = s2.station_id
WHERE LOWER(TRIM(s1.station_name)) LIKE 'mumbai%'
AND LOWER(TRIM(s2.station_name)) LIKE 'pune%'
AND r1.stop_number < r2.stop_number;

SELECT DISTINCT 
t1.train_name AS First_Train,
t2.train_name AS Second_Train,
sj.station_name AS Junction
FROM ROUTE r1
JOIN ROUTE rj1 ON r1.train_id = rj1.train_id
JOIN ROUTE rj2 ON rj1.station_id = rj2.station_id
JOIN ROUTE r2 ON r2.train_id = rj2.train_id
JOIN TRAIN t1 ON t1.train_id = r1.train_id
JOIN TRAIN t2 ON t2.train_id = r2.train_id
JOIN STATION s1 ON r1.station_id = s1.station_id
JOIN STATION s2 ON r2.station_id = s2.station_id
JOIN STATION sj ON rj1.station_id = sj.station_id
WHERE LOWER(TRIM(s1.station_name)) LIKE 'mumbai%'
AND LOWER(TRIM(s2.station_name)) LIKE 'chennai%'
AND r1.stop_number < rj1.stop_number
AND rj2.stop_number < r2.stop_number
AND r1.train_id <> r2.train_id;

SELECT 
b.booking_id,
u.user_name,
t.train_name,
b.journey_date,
b.seat_count,
b.booking_status
FROM Booking b
JOIN User u ON b.user_id = u.user_id
JOIN Train t ON b.train_id = t.train_id;

SELECT 
train_id,
COUNT(*) AS total_bookings
FROM Booking
GROUP BY train_id;

-- update1 seat count
UPDATE Booking
SET seat_count = seat_count + 1
WHERE booking_id = 1;
-- check incremented seat count 
SELECT booking_id, seat_count 
FROM Booking 
WHERE booking_id = 1;

-- update2 change journey date 
SELECT booking_id, journey_date FROM Booking WHERE booking_id = 1;

UPDATE Booking
SET journey_date = DATE_ADD(journey_date, INTERVAL 1 DAY)
WHERE booking_id = 1;

SELECT booking_id, journey_date FROM Booking WHERE booking_id = 1;

-- delete 1 booking 
DELETE FROM Ticket
WHERE booking_id = 1;

DELETE FROM Booking
WHERE booking_id = 1;

SELECT * FROM Booking WHERE booking_id = 1;
SELECT * FROM Ticket WHERE booking_id = 1;

-- delete 2 cancelled booking status
SELECT booking_id, booking_status 
FROM Booking 
WHERE booking_status = 'Cancelled';

SELECT COUNT(*) AS cancelled_count 
FROM Booking 
WHERE booking_status = 'Cancelled';

DELETE FROM Booking
WHERE booking_status = 'Cancelled';
