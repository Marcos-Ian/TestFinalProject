-- Schema for Hotel Reservation System (MySQL)

CREATE DATABASE IF NOT EXISTS hotel_reservation;
USE hotel_reservation;

-- Table: guest
CREATE TABLE guest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    firstName VARCHAR(100),
    lastName VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(150),
    loyaltyNumber VARCHAR(100),
    address VARCHAR(255)
);

-- Table: loyalty_account (optional extension for loyalty tracking)
CREATE TABLE loyalty_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    loyaltyNumber VARCHAR(100) NOT NULL,
    points INT DEFAULT 0,
    status VARCHAR(50),
    CONSTRAINT fk_loyalty_guest FOREIGN KEY (guest_id) REFERENCES guest(id),
    CONSTRAINT uq_loyalty_number UNIQUE (loyaltyNumber)
);

-- Table: room_type
CREATE TABLE room_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('SINGLE', 'DOUBLE', 'DELUXE', 'PENTHOUSE') NOT NULL UNIQUE,
    basePrice DECIMAL(10,2) NOT NULL,
    capacity INT NOT NULL
);

-- Table: reservation
CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    checkIn DATE,
    checkOut DATE,
    status ENUM('BOOKED', 'CANCELLED', 'CHECKED_OUT', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED'),
    discount_percent DOUBLE DEFAULT 0.0,
    total_amount DECIMAL(10,2) DEFAULT 0.0,
    CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id) REFERENCES guest(id)
);

-- Join table: reservation_room
CREATE TABLE reservation_room (
    reservation_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    PRIMARY KEY (reservation_id, room_type_id),
    CONSTRAINT fk_res_room_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    CONSTRAINT fk_res_room_roomtype FOREIGN KEY (room_type_id) REFERENCES room_type(id)
);

-- Table: reservation_addon
CREATE TABLE reservation_addon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    addOnName VARCHAR(150) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    perNight BOOLEAN NOT NULL,
    CONSTRAINT fk_addon_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id)
);

-- Table: payment
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    method ENUM('CASH', 'CARD', 'LOYALTY_POINTS') NOT NULL,
    type ENUM('NORMAL', 'DEPOSIT', 'REFUND') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(150),
    notes VARCHAR(500),
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id)
);

-- Table: feedback
CREATE TABLE feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_email VARCHAR(150) NOT NULL,
    reservation_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comments VARCHAR(2000),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_feedback_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id)
);

-- Table: waitlist_entry
CREATE TABLE waitlist_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_name VARCHAR(150) NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    desired_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table: admin_user
CREATE TABLE admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    passwordHash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'MANAGER')
);

-- Table: activity_log
CREATE TABLE activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor VARCHAR(150) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    message VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed data: default admin user (password hash is a placeholder, replace with a real BCrypt hash)
INSERT INTO admin_user (username, passwordHash, role)
VALUES ('admin', '$2a$10$PLACEHOLDER_HASH_REPLACE_ME', 'ADMIN');
