package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Guest;
import model.Reservation;
import model.RoomType;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import service.RoomService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller for the hotel kiosk self-service booking system.
 * Manages the complete booking flow from welcome screen through confirmation.
 */
public class KioskController {
    private static final Logger LOGGER = Logger.getLogger(KioskController.class.getName());

    // Services (injected via constructor or Bootstrap)
    private final ReservationService reservationService;
    private final RoomService roomService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;

    // Welcome Screen Controls
    @FXML private Button rulesButton;
    @FXML private Button startBookingButton;
    @FXML private ImageView instructionalImage;

    // Current booking state
    private int adults = 0;
    private int children = 0;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Guest currentGuest;
    private List<RoomType> selectedRooms = new ArrayList<>();
    private List<String> selectedAddOns = new ArrayList<>();
    private double estimatedTotal = 0.0;

    /**
     * Constructor with dependency injection
     */
    public KioskController() {
        // Get services from Bootstrap (DI container)
        this.reservationService = Bootstrap.getReservationService();
        this.roomService = Bootstrap.getRoomService();
        this.loyaltyService = Bootstrap.getLoyaltyService();
        this.billingContext = Bootstrap.getBillingContext();

        LOGGER.info("KioskController initialized");
    }

    /**
     * Alternative constructor for testing or direct injection
     */
    public KioskController(ReservationService reservationService, RoomService roomService,
                           LoyaltyService loyaltyService, BillingContext billingContext) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.loyaltyService = loyaltyService;
        this.billingContext = billingContext;

        LOGGER.info("KioskController initialized with injected services");
    }

    /**
     * Initialize method called after FXML is loaded
     */
    @FXML
    public void initialize() {
        LOGGER.info("Initializing Kiosk Welcome Screen");

        // Set up welcome screen
        setupWelcomeScreen();

        // Set up button handlers
        setupButtonHandlers();
    }

    /**
     * Set up the welcome screen with instructional content
     */
    private void setupWelcomeScreen() {
        try {
            // Load instructional image/GIF if available
            // You can replace this with your actual image path
            // instructionalImage.setImage(new Image("/images/instructions.gif"));

            LOGGER.info("Welcome screen setup complete");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load instructional image", e);
        }
    }

    /**
     * Set up button event handlers
     */
    private void setupButtonHandlers() {
        // Rules button - always accessible
        rulesButton.setOnAction(event -> showRulesAndRegulations());

        // Start booking button - begins the booking flow
        startBookingButton.setOnAction(event -> startBookingFlow());

        LOGGER.info("Button handlers configured");
    }

    /**
     * Show rules and regulations dialog
     */
    private void showRulesAndRegulations() {
        LOGGER.info("Displaying rules and regulations");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rules & Regulations");
        alert.setHeaderText("Grand Plaza Hotel - Guest Guidelines");

        String rules = """
                ROOM OCCUPANCY RULES:
                • Single Room: Maximum 2 guests
                • Double Room: Maximum 4 guests
                • Deluxe Room: Maximum 2 guests
                • Penthouse: Maximum 2 guests
                
                CHECK-IN/CHECK-OUT:
                • Check-in: 3:00 PM
                • Check-out: 11:00 AM
                • Early check-in subject to availability
                
                BOOKING POLICY:
                • Valid ID required at check-in
                • Payment processed at front desk
                • Cancellations must be made 24 hours in advance
                
                GENERAL RULES:
                • No smoking in rooms
                • Quiet hours: 10:00 PM - 7:00 AM
                • Pets allowed with prior approval
                • Damage charges may apply
                """;

        alert.setContentText(rules);
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    /**
     * Start the booking flow - Step 1: Guest Count
     */
    private void startBookingFlow() {
        LOGGER.info("Starting booking flow");

        try {
            // Load the guest count screen
            navigateToScreen("/view/KioskGuestCount.fxml", "Guest Information");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start booking flow", e);
            showError("System Error", "Unable to start booking. Please try again.");
        }
    }

    /**
     * Step 2: Collect check-in and check-out dates
     */
    public void proceedToDateSelection(int adults, int children) {
        this.adults = adults;
        this.children = children;

        LOGGER.info(String.format("Guest count collected: %d adults, %d children", adults, children));

        try {
            navigateToScreen("/view/KioskDateSelection.fxml", "Select Dates");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to date selection", e);
            showError("Navigation Error", "Unable to proceed. Please try again.");
        }
    }

    /**
     * Step 3: Room selection based on availability
     */
    public void proceedToRoomSelection(LocalDate checkIn, LocalDate checkOut) {
        this.checkInDate = checkIn;
        this.checkOutDate = checkOut;

        // Validate dates
        if (!validateDates(checkIn, checkOut)) {
            return;
        }

        LOGGER.info(String.format("Dates selected: %s to %s", checkIn, checkOut));

        try {
            // Check room availability
            List<RoomType> availableRooms = roomService.getAvailableRooms(checkIn, checkOut);

            if (availableRooms.isEmpty()) {
                showError("No Availability", "No rooms available for selected dates. Please try different dates.");
                return;
            }

            // Navigate to room selection
            navigateToScreen("/view/KioskRoomSelection.fxml", "Select Rooms");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check room availability", e);
            showError("System Error", "Unable to check availability. Please try again.");
        }
    }

    /**
     * Step 4: Collect guest details
     */
    public void proceedToGuestDetails(List<RoomType> rooms) {
        this.selectedRooms = rooms;

        // Validate occupancy
        if (!validateOccupancy(rooms)) {
            return;
        }

        LOGGER.info(String.format("Rooms selected: %d rooms", rooms.size()));

        try {
            navigateToScreen("/view/KioskGuestDetails.fxml", "Guest Information");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to guest details", e);
            showError("Navigation Error", "Unable to proceed. Please try again.");
        }
    }

    /**
     * Step 5: Select add-on services
     */
    public void proceedToAddOns(Guest guest) {
        this.currentGuest = guest;

        // Validate guest details
        if (!validateGuest(guest)) {
            return;
        }

        LOGGER.info(String.format("Guest details collected: %s %s", guest.getFirstName(), guest.getLastName()));

        try {
            navigateToScreen("/view/KioskAddOns.fxml", "Add-On Services");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to add-ons", e);
            showError("Navigation Error", "Unable to proceed. Please try again.");
        }
    }

    /**
     * Step 6: Review and confirm booking
     */
    public void proceedToConfirmation(List<String> addOns) {
        this.selectedAddOns = addOns;

        LOGGER.info(String.format("Add-ons selected: %d services", addOns.size()));

        try {
            // Calculate total estimate
            estimatedTotal = billingContext.calculateTotal(selectedRooms, selectedAddOns,
                    checkInDate, checkOutDate);

            navigateToScreen("/view/KioskConfirmation.fxml", "Confirm Booking");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to calculate estimate", e);
            showError("Calculation Error", "Unable to calculate total. Please try again.");
        }
    }

    /**
     * Final step: Save reservation and show confirmation
     */
    public void confirmBooking() {
        LOGGER.info("Confirming booking");

        try {
            // Create and save reservation
            Reservation reservation = new Reservation();
            reservation.setGuest(currentGuest);
            reservation.setCheckIn(checkInDate);
            reservation.setCheckOut(checkOutDate);
            reservation.setStatus("CONFIRMED");

            // Save through service
            Reservation saved = reservationService.createReservation(reservation, selectedRooms, selectedAddOns);

            LOGGER.info(String.format("Reservation created successfully: ID %d", saved.getId()));

            // Show success message
            showConfirmation(saved);

            // Reset kiosk after delay
            resetKiosk();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save reservation", e);
            showError("Booking Error", "Unable to complete booking. Please see front desk.");
        }
    }

    /**
     * Validate date selection
     */
    private boolean validateDates(LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now();

        if (checkIn.isBefore(today)) {
            showError("Invalid Date", "Check-in date cannot be in the past.");
            return false;
        }

        if (checkOut.isBefore(checkIn.plusDays(1))) {
            showError("Invalid Date", "Check-out must be at least one day after check-in.");
            return false;
        }

        return true;
    }

    /**
     * Validate occupancy rules
     */
    private boolean validateOccupancy(List<RoomType> rooms) {
        int totalGuests = adults + children;
        int totalCapacity = rooms.stream()
                .mapToInt(RoomType::getCapacity)
                .sum();

        if (totalGuests > totalCapacity) {
            showError("Occupancy Error",
                    String.format("Selected rooms can accommodate %d guests, but you have %d guests. " +
                            "Please select additional rooms.", totalCapacity, totalGuests));
            return false;
        }

        // Validate individual room occupancy
        for (RoomType room : rooms) {
            if (room.getCapacity() < 1) {
                showError("Invalid Room", "Room type has invalid capacity.");
                return false;
            }
        }

        return true;
    }

    /**
     * Validate guest information
     */
    private boolean validateGuest(Guest guest) {
        if (guest.getFirstName() == null || guest.getFirstName().trim().isEmpty()) {
            showError("Invalid Input", "First name is required.");
            return false;
        }

        if (guest.getLastName() == null || guest.getLastName().trim().isEmpty()) {
            showError("Invalid Input", "Last name is required.");
            return false;
        }

        if (guest.getPhone() == null || !guest.getPhone().matches("\\d{10}")) {
            showError("Invalid Input", "Please enter a valid 10-digit phone number.");
            return false;
        }

        if (guest.getEmail() == null || !guest.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Invalid Input", "Please enter a valid email address.");
            return false;
        }

        return true;
    }

    /**
     * Navigate to a different screen
     */
    private void navigateToScreen(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        // Pass this controller to the next screen if needed
        Object controller = loader.getController();
        if (controller instanceof KioskScreenController) {
            ((KioskScreenController) controller).setMainController(this);
        }

        Stage stage = (Stage) startBookingButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        LOGGER.warning(String.format("Error shown to user: %s - %s", title, message));
    }

    /**
     * Show confirmation dialog
     */
    private void showConfirmation(Reservation reservation) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Confirmed");
        alert.setHeaderText("Your reservation has been confirmed!");

        String confirmationMessage = String.format("""
                Reservation ID: %d
                Guest: %s %s
                Check-in: %s
                Check-out: %s
                
                Estimated Total: $%.2f
                (Tax and fees included)
                
                IMPORTANT: Please proceed to the front desk for payment and check-in.
                Your rooms will be ready at 3:00 PM.
                
                Thank you for choosing Grand Plaza Hotel!
                """,
                reservation.getId(),
                currentGuest.getFirstName(),
                currentGuest.getLastName(),
                checkInDate,
                checkOutDate,
                estimatedTotal);

        alert.setContentText(confirmationMessage);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * Reset kiosk to welcome screen
     */
    private void resetKiosk() {
        LOGGER.info("Resetting kiosk to welcome screen");

        // Clear booking state
        adults = 0;
        children = 0;
        checkInDate = null;
        checkOutDate = null;
        currentGuest = null;
        selectedRooms.clear();
        selectedAddOns.clear();
        estimatedTotal = 0.0;

        try {
            // Return to welcome screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/KioskWelcome.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) startBookingButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Welcome - Grand Plaza Hotel");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to reset kiosk", e);
        }
    }

    /**
     * Get current booking state (for use by sub-controllers)
     */
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public Guest getCurrentGuest() { return currentGuest; }
    public List<RoomType> getSelectedRooms() { return selectedRooms; }
    public List<String> getSelectedAddOns() { return selectedAddOns; }
    public double getEstimatedTotal() { return estimatedTotal; }

    /**
     * Get services (for use by sub-controllers)
     */
    public ReservationService getReservationService() { return reservationService; }
    public RoomService getRoomService() { return roomService; }
    public LoyaltyService getLoyaltyService() { return loyaltyService; }
    public BillingContext getBillingContext() { return billingContext; }
}

/**
 * Interface for sub-controllers that need reference to main controller
 */
interface KioskScreenController {
    void setMainController(KioskController mainController);
}