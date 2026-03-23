show databases;
CREATE DATABASE ConnectingTrainDB;
USE ConnectingTrainDB;
CREATE TABLE User (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(15) NOT NULL
);
CREATE TABLE Train (
    train_id INT AUTO_INCREMENT PRIMARY KEY,
    train_number VARCHAR(10) UNIQUE NOT NULL,
    train_name VARCHAR(100) NOT NULL,
    train_type VARCHAR(30),
    days_of_run VARCHAR(50)
);
CREATE TABLE Station (
    station_id INT AUTO_INCREMENT PRIMARY KEY,
    station_code VARCHAR(10) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    state VARCHAR(50),
    zone VARCHAR(20),
    station_type VARCHAR(20)
    CHECK (station_type IN ('Junction','Terminal','Halt'))
);
CREATE TABLE Route (
    route_id INT AUTO_INCREMENT PRIMARY KEY,
    train_id INT NOT NULL,
    station_id INT NOT NULL,
    stop_number INT NOT NULL,
    arrival_time TIME,
    departure_time TIME,
    halt_duration INT,
    distance_from_source INT,

    FOREIGN KEY (train_id) REFERENCES Train(train_id),
    FOREIGN KEY (station_id) REFERENCES Station(station_id)
);
CREATE TABLE Stop (
    stop_id INT AUTO_INCREMENT PRIMARY KEY,
    route_id INT NOT NULL,
    station_id INT NOT NULL,
    stop_sequence INT NOT NULL,
    arrival_time TIME,
    departure_time TIME,
    halt_duration INT,

    FOREIGN KEY (route_id) REFERENCES Route(route_id),
    FOREIGN KEY (station_id) REFERENCES Station(station_id)
);
CREATE TABLE Booking (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    train_id INT NOT NULL,
    source_station_id INT NOT NULL,
    destination_station_id INT NOT NULL,
    journey_date DATE NOT NULL,
    booking_date DATE NOT NULL,
    seat_count INT NOT NULL,
    booking_status VARCHAR(20)
    CHECK (booking_status IN ('Confirmed','Pending','Cancelled')),

    FOREIGN KEY (user_id) REFERENCES User(user_id),
    FOREIGN KEY (train_id) REFERENCES Train(train_id),
    FOREIGN KEY (source_station_id) REFERENCES Station(station_id),
    FOREIGN KEY (destination_station_id) REFERENCES Station(station_id)
);
CREATE TABLE Ticket (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    coach_no VARCHAR(10),
    seat_no VARCHAR(10),
    fare DECIMAL(8,2),

    FOREIGN KEY (booking_id) REFERENCES Booking(booking_id)
);
CREATE TABLE Train_Raw (
    train_no INT,
    train_name VARCHAR(100),
    seq INT,
    station_code VARCHAR(10),
    station_name VARCHAR(100),
    arrival_time TIME,
    departure_time TIME,
    distance INT,
    source_station_code VARCHAR(10),
    source_station_name VARCHAR(100),
    destination_station_code VARCHAR(10),
    destination_station_name VARCHAR(100)
);

INSERT INTO Train (train_number, train_name)
SELECT DISTINCT train_no, train_name
FROM Train_Raw;

INSERT INTO Station (station_code, station_name)
SELECT DISTINCT station_code, station_name
FROM Train_Raw;

INSERT INTO Route (train_id, station_id, stop_number, arrival_time, departure_time, distance_from_source)
SELECT 
    t.train_id,
    s.station_id,
    r.seq,
    r.arrival_time,
    r.departure_time,
    r.distance
FROM Train_Raw r
JOIN Train t ON t.train_number = r.train_no
JOIN Station s ON s.station_code = r.station_code;

ALTER TABLE Train
MODIFY train_number INT;

INSERT INTO Stop (route_id, station_id, stop_sequence, arrival_time, departure_time)
SELECT 
    rt.route_id,
    s.station_id,
    r.seq,
    r.arrival_time,
    r.departure_time
FROM Train_Raw r
JOIN Train t ON t.train_number = r.train_no
JOIN Station s ON s.station_code = r.station_code
JOIN Route rt 
    ON rt.train_id = t.train_id 
    AND rt.station_id = s.station_id;
    
INSERT INTO User (user_name, email, phone_number)
VALUES ('Mishty Kataria', 'mishty@gmail.com', '9876543210');

INSERT INTO Booking 
(user_id, train_id, source_station_id, destination_station_id,
 journey_date, booking_date, seat_count, booking_status)
VALUES 
(
 1,
 (SELECT train_id FROM Train WHERE train_number = '107'),
 (SELECT station_id FROM Station WHERE station_code = 'BRC'),
 (SELECT station_id FROM Station WHERE station_code = 'PUNE'),
 '2026-04-01',
 CURDATE(),
 1,
 'Confirmed'
);

INSERT INTO Ticket
(booking_id, passenger_name, coach_no, seat_no, fare)
VALUES
(LAST_INSERT_ID(), 'Mishty Kataria', 'S1', '21', 550.00);

UPDATE Station 
SET state = 'Gujarat', zone = 'WR', station_type = 'Junction'
WHERE station_code = 'BRC';

UPDATE Station 
SET state = 'Gujarat', zone = 'WR', station_type = 'Junction'
WHERE station_code = 'ST';

UPDATE Station 
SET state = 'Maharashtra', zone = 'WR', station_type = 'Terminal'
WHERE station_code = 'BCT';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Junction'
WHERE station_code = 'KYN';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Halt'
WHERE station_code = 'LNL';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Terminal'
WHERE station_code = 'PUNE';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Terminal'
WHERE station_code = 'CSMT';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Junction'
WHERE station_code = 'DR';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Junction'
WHERE station_code = 'TNA';

UPDATE Station 
SET state = 'Maharashtra', zone = 'CR', station_type = 'Junction'
WHERE station_code = 'PNVL';

UPDATE Route
SET halt_duration = TIMESTAMPDIFF(
    MINUTE,
    arrival_time,
    departure_time
)
WHERE arrival_time IS NOT NULL 
  AND departure_time IS NOT NULL
  AND route_id > 0;
  
  UPDATE Stop
SET halt_duration = TIMESTAMPDIFF(
    MINUTE,
    arrival_time,
    departure_time
)
WHERE arrival_time IS NOT NULL 
  AND departure_time IS NOT NULL
  AND route_id > 0;
  
SELECT * FROM TRAIN;

SELECT 
    t.train_number,
    t.train_name,
    s1.station_name AS source,
    s2.station_name AS destination,
    r1.departure_time,
    r2.arrival_time
FROM Route r1
JOIN Route r2 
    ON r1.train_id = r2.train_id 
    AND r1.stop_number < r2.stop_number
JOIN Train t 
    ON t.train_id = r1.train_id
JOIN Station s1 
    ON s1.station_id = r1.station_id
JOIN Station s2 
    ON s2.station_id = r2.station_id
WHERE s1.station_code = 'BRC'   -- Source (e.g., BRC)
  AND s2.station_code = 'PUNE';  -- Destination (e.g., PUNE)

--  CONNECTING TRAIN QUERY
SELECT 
    t1.train_number AS train1,
    s1.station_name AS source,
    s_mid.station_name AS via_station,
    t2.train_number AS train2,
    s2.station_name AS destination
FROM Route r1
JOIN Route r_mid1 
    ON r1.train_id = r_mid1.train_id 
    AND r1.stop_number < r_mid1.stop_number

JOIN Route r_mid2 
    ON r_mid1.station_id = r_mid2.station_id

JOIN Route r2 
    ON r_mid2.train_id = r2.train_id 
    AND r_mid2.stop_number < r2.stop_number

JOIN Train t1 ON t1.train_id = r1.train_id
JOIN Train t2 ON t2.train_id = r2.train_id

JOIN Station s1 ON s1.station_id = r1.station_id
JOIN Station s_mid ON s_mid.station_id = r_mid1.station_id
JOIN Station s2 ON s2.station_id = r2.station_id

WHERE s1.station_code = 'BRC'   -- Source
  AND s2.station_code = 'PUNE'   -- Destination
  AND r1.train_id != r2.train_id;

-- VIEWBOOKING
SELECT 
    b.booking_id,u.user_name,u.email,tr.train_number,tr.train_name,s1.station_name AS source,s2.station_name AS destination,b.journey_date,
    b.booking_date,b.seat_count,b.booking_status,t.passenger_name,t.coach_no,t.seat_no,t.fare
FROM Booking b
JOIN User u 
    ON u.user_id = b.user_id
JOIN Train tr 
    ON tr.train_id = b.train_id
JOIN Station s1 
    ON s1.station_id = b.source_station_id
JOIN Station s2 
    ON s2.station_id = b.destination_station_id
LEFT JOIN Ticket t 
    ON t.booking_id = b.booking_id;
