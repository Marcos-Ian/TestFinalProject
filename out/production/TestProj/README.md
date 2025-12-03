# Hotel Reservation System

## Introduction
This is the final project in my Java class. 

Hotel ABC, located in XYZ city, previously used a **manual reservation and billing system**, which caused inefficiencies and risks such as lost records, extra storage requirements, and human errors in billing.  

This project implements a **computerized Hotel Reservation System** that allows hotel staff to quickly manage reservations, maintain secure records, and provide a smooth booking and billing experience for guests.

## Objectives
- Replace the manual reservation system with a computerized solution.
- Save time for hotel employees and guests.
- Develop data structures to store guest details efficiently.
- Create a user-friendly interface for both guests and staff.
- Allow guests to book rooms via **kiosk** or **phone**.
- Provide automatic billing at check-out.

## Functional Requirements

### Guest Booking (Kiosk/Self-service)
- Welcome page with a friendly message.
- Display available rooms read from the database.
- Ask for the number of guests and suggest the required number of rooms.
- Limit of 2 adults per room; allow single-person bookings.
- Collect guest details with proper validation.
- Record check-in/check-out dates.
- Confirm booking and store information in the database.
- Show confirmation and welcome message to the guest.

### Guest Booking (Phone/Staff-assisted)
- Admin login page for staff only.
- Collect guest information and booking details similar to kiosk process.

### Admin Functions
- Search for guests by name or phone number.
- Offer discounts during billing (up to 25%).
- Cancel bookings if needed.
- Check out guests and generate bills.

## Minimum Classes
The project includes the following core classes:

| Class | Attributes |
|-------|------------|
| `Guest` | Guest_ID, Title, First_name, Last_name, Address, Phone, Email |
| `Room` | Room_ID, Room_type, Rate |
| `SingleRoom` | Room_ID, Rate |
| `DoubleRoom` | Room_ID, Rate |
| `DeluxRoom` | Room_ID, Rate |
| `PentHouse` | Room_ID, Rate |
| `Reservation` | Book_ID, Book_date, Check_in, Check_out |
| `Bill` | Bill_ID, Amount_toPay |
| `Login` | Login_ID, Login_pswd |


## User Interface Flow

### Main Menu
```
Hotel Reservation System
[1] Book a Room
[2] Bill Service
[3] Current Bookings
[4] Available Rooms
[5] Exit
```

#### Booking Flow
1. Enter number of guests.
2. System suggests available rooms.
3. Select room type and number of days.
4. Enter guest information (email must be valid).
5. System confirms booking and stores data.

#### Billing Flow
- Enter Booking ID.
- Display guest information, room details, discounts, and total amount.

## Features
- Computerized system with a user-friendly interface.
- Handles room booking, billing, and record keeping efficiently.
- Supports guest searches, cancellations, and discount applications.
- Flexible room assignment based on number of guests.

## Technologies Used
- Programming Language: Java
- Database: SQLite
- Object-Oriented Design: Classes with proper data encapsulation and validation.

## How to Run
1. Clone the repository:
```bash
git clone https://github.com/dliu84/Hotel-Reservation-System.git
```
2. Open the project in your IDE (I used Eclipse and SceneBuilder in this project)
3. Compile and run the main program file.
4. Follow on-screen instructions to book rooms, check bills, and manage the hotel system.

## Contributors

- **Professor Mahboob Ali** (Instructor, Seneca Polytechnic, APD545, 2024 Winter)  
- **Di Liu** - [dliu84](https://github.com/dliu84)  

## License

This project is licensed under the [MIT License](LICENSE).
