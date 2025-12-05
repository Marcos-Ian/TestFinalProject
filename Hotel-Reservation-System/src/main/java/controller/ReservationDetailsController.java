package controller;

import app.Bootstrap;
import config.LoyaltyConfig;
import config.PricingConfig;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Guest;
import model.Reservation;
import model.RoomType;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import security.AdminUser;
import security.AuthenticationService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced controller for viewing complete reservation details with discount support
 */
public class ReservationDetailsController {
    private static final Logger LOGGER = Logger.getLogger(ReservationDetailsController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final BillingContext billingContext;
    private final LoyaltyService loyaltyService;
    private final ReservationService reservationService;
    private final AuthenticationService authService;

    private Reservation currentReservation;
    private final ObservableList<RoomRow> roomRows = FXCollections.observableArrayList();
    private final ObservableList<AddOnRow> addOnRows = FXCollections.observableArrayList();
    private final ObservableList<PaymentRow> paymentRows = FXCollections.observableArrayList();

    // Static pricing map
    private static final Map<String, Double> ADD_ON_PRICES = new HashMap<>();
    private static final Map<String, Boolean> ADD_ON_PER_NIGHT = new HashMap<>();

    static {
        ADD_ON_PRICES.put("WiFi", 10.0);
        ADD_ON_PRICES.put("Breakfast", 25.0);
        ADD_ON_PRICES.put("Parking", 15.0);
        ADD_ON_PRICES.put("Spa", 100.0);

        ADD_ON_PER_NIGHT.put("WiFi", true);
        ADD_ON_PER_NIGHT.put("Breakfast", true);
        ADD_ON_PER_NIGHT.put("Parking", true);
        ADD_ON_PER_NIGHT.put("Spa", false);
    }

    // ─────────── TOP BAR ───────────
    @FXML private Button backButton;
    @FXML private Label titleLabel;
    @FXML private Label reservationIdLabel;
    @FXML private Label statusBadge;

    // ─────────── GUEST DETAILS ───────────
    @FXML private Label guestNameLabel;
    @FXML private Label guestPhoneLabel;
    @FXML private Label guestEmailLabel;
    @FXML private Label loyaltyStatusLabel;
    @FXML private Button enrollLoyaltyButton;
    @FXML private Label loyaltyNumberLabel;
    @FXML private Label loyaltyNumberSection;
    @FXML private Label loyaltyPointsSection;

    @FXML private Label loyaltyPointsLabel;

    // ─────────── RESERVATION DETAILS ───────────
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label nightsLabel;
    @FXML private Label guestsLabel;
    @FXML private Label bookingDateLabel;
    @FXML private Label bookedByLabel;

    // ─────────── ROOM TABLE ───────────
    @FXML private TableView<RoomRow> roomsTable;
    @FXML private TableColumn<RoomRow, String> roomTypeCol;
    @FXML private TableColumn<RoomRow, Integer> roomQuantityCol;
    @FXML private TableColumn<RoomRow, Integer> roomCapacityCol;
    @FXML private TableColumn<RoomRow, Double> roomPriceCol;
    @FXML private TableColumn<RoomRow, Double> roomTotalCol;

    // ─────────── ADD-ON SERVICES TABLE ───────────
    @FXML private TableView<AddOnRow> addonsTable;
    @FXML private TableColumn<AddOnRow, String> addonNameCol;
    @FXML private TableColumn<AddOnRow, String> addonPricingCol;
    @FXML private TableColumn<AddOnRow, Double> addonPriceCol;
    @FXML private TableColumn<AddOnRow, Double> addonTotalCol;

    // ─────────── FINANCIAL SUMMARY ───────────
    @FXML private Label roomSubtotalLabel;
    @FXML private Label addonsTotalLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label discountRow;
    @FXML private Label discountLabel;
    @FXML private Label loyaltyRedemptionRow;
    @FXML private Label loyaltyRedemptionLabel;
    @FXML private Label grandTotalLabel;
    @FXML private Label paidLabel;
    @FXML private Label balanceLabel;

    // ─────────── PAYMENTS TABLE ───────────
    @FXML private TableView<PaymentRow> paymentsTable;
    @FXML private TableColumn<PaymentRow, String> paymentDateCol;
    @FXML private TableColumn<PaymentRow, String> paymentTypeCol;
    @FXML private TableColumn<PaymentRow, Double> paymentAmountCol;
    @FXML private TableColumn<PaymentRow, String> paymentByCol;
    @FXML private TableColumn<PaymentRow, String> paymentNotesCol;

    // ─────────── ACTION BUTTONS ───────────
    @FXML private Button modifyButton;
    @FXML private Button applyDiscountButton;
    @FXML private Button processPaymentButton;
    @FXML private Button checkoutButton;
    @FXML private Button cancelButton;

    public ReservationDetailsController() {
        BillingContext context;
        LoyaltyService loyalty;
        ReservationService resService;
        AuthenticationService auth;

        try {
            context = Bootstrap.getBillingContext();
            loyalty = Bootstrap.getLoyaltyService();
            resService = Bootstrap.getReservationService();
            auth = Bootstrap.getAuthenticationService();
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Fallback initialization", ex);
            context = new BillingContext();
            loyalty = new LoyaltyService(new LoyaltyConfig());
            resService = null;
            auth = null;
        }

        this.billingContext = context;
        this.loyaltyService = loyalty;
        this.reservationService = resService;
        this.authService = auth;
    }

    @FXML
    public void initialize() {
        configureTables();
        configureButtons();
        syncSectionVisibility();
    }

    private void syncSectionVisibility() {
        if (loyaltyNumberSection != null) {
            loyaltyNumberSection.setManaged(loyaltyNumberSection.isVisible());
        }
        if (loyaltyPointsSection != null) {
            loyaltyPointsSection.setManaged(loyaltyPointsSection.isVisible());
        }
    }

    private void configureTables() {
        if (roomsTable != null && roomTypeCol != null) {
            roomTypeCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getRoomType()));
            roomQuantityCol.setCellValueFactory(cellData ->
                    new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
            roomCapacityCol.setCellValueFactory(cellData ->
                    new SimpleIntegerProperty(cellData.getValue().getCapacity()).asObject());
            roomPriceCol.setCellValueFactory(cellData ->
                    new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
            roomTotalCol.setCellValueFactory(cellData ->
                    new SimpleDoubleProperty(cellData.getValue().getTotal()).asObject());
            roomsTable.setItems(roomRows);
        }

        if (addonsTable != null && addonNameCol != null) {
            addonNameCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getName()));
            addonPricingCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getPricingModel()));
            addonPriceCol.setCellValueFactory(cellData ->
                    new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
            addonTotalCol.setCellValueFactory(cellData ->
                    new SimpleDoubleProperty(cellData.getValue().getTotal()).asObject());
            addonsTable.setItems(addOnRows);
        }

        if (paymentsTable != null && paymentDateCol != null) {
            paymentDateCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getDate()));
            paymentTypeCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getPaymentType()));
            paymentAmountCol.setCellValueFactory(cellData ->
                    new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
            paymentByCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getProcessedBy()));
            paymentNotesCol.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getNotes()));
            paymentsTable.setItems(paymentRows);
        }
    }

    private void configureButtons() {
        if (backButton != null) {
            backButton.setOnAction(event -> handleBack());
        }
        if (modifyButton != null) {
            modifyButton.setOnAction(event -> handleModify());
        }
        if (applyDiscountButton != null) {
            applyDiscountButton.setOnAction(event -> handleApplyDiscount());
        }
        if (processPaymentButton != null) {
            processPaymentButton.setOnAction(event -> handleProcessPayment());
        }
        if (checkoutButton != null) {
            checkoutButton.setOnAction(event -> handleCheckout());
        }
        if (cancelButton != null) {
            cancelButton.setOnAction(event -> handleCancel());
        }
    }

    /**
     * Display complete reservation details
     */
    public void displayReservation(Reservation reservation, List<RoomType> rooms,
                                   List<String> addOns, List<PaymentRow> payments,
                                   int totalGuests, double amountPaid, String bookedBy) {
        this.currentReservation = reservation;
        this.paymentRows.setAll(payments);

        populateGuestSection(reservation.getGuest());
        populateReservationSection(reservation, totalGuests, bookedBy);
        populateRoomTable(rooms, reservation.getCheckIn(), reservation.getCheckOut());
        populateAddOnTable(addOns, reservation.getCheckIn(), reservation.getCheckOut());
        updateFinancials(reservation, rooms, addOns, amountPaid);
    }

    private void populateGuestSection(Guest guest) {
        guestNameLabel.setText(guest.getFirstName() + " " + guest.getLastName());
        guestPhoneLabel.setText(guest.getPhone() != null ? guest.getPhone() : "N/A");
        guestEmailLabel.setText(guest.getEmail() != null ? guest.getEmail() : "N/A");

        boolean isMember = guest.getLoyaltyNumber() != null && !guest.getLoyaltyNumber().isEmpty();
        loyaltyStatusLabel.setText(isMember ? "Yes" : "No");

        if (enrollLoyaltyButton != null) {
            enrollLoyaltyButton.setVisible(!isMember);
            enrollLoyaltyButton.setManaged(!isMember);
        }

        loyaltyNumberSection.setVisible(isMember);
        loyaltyNumberSection.setManaged(isMember);
        loyaltyPointsSection.setVisible(isMember);
        loyaltyPointsSection.setManaged(isMember);

        if (isMember) {
            loyaltyNumberLabel.setText(guest.getLoyaltyNumber());
            int points = loyaltyService.calculateEarnedPoints(500.0); // Example
            loyaltyPointsLabel.setText(String.valueOf(points));
        }

        syncSectionVisibility();
    }

    private void populateReservationSection(Reservation reservation, int guestCount, String bookedBy) {
        titleLabel.setText("Reservation Details");
        reservationIdLabel.setText(reservation.getId() != null ? "ID: #" + reservation.getId() : "ID: Pending");
        statusBadge.setText(reservation.getStatus() != null ? reservation.getStatus().name() : "");

        checkInLabel.setText(formatDate(reservation.getCheckIn()));
        checkOutLabel.setText(formatDate(reservation.getCheckOut()));

        long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        nightsLabel.setText(String.valueOf(nights));
        guestsLabel.setText(String.valueOf(guestCount));
        bookingDateLabel.setText(formatDate(LocalDate.now()));
        bookedByLabel.setText(bookedBy);
    }

    private void populateRoomTable(List<RoomType> rooms, LocalDate checkIn, LocalDate checkOut) {
        roomRows.clear();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        Map<RoomType.Type, List<RoomType>> grouped = new HashMap<>();
        for (RoomType room : rooms) {
            grouped.computeIfAbsent(room.getType(), key -> new ArrayList<>()).add(room);
        }

        grouped.forEach((type, roomList) -> {
            RoomType sample = roomList.get(0);
            int quantity = roomList.size();
            double subtotal = sample.getBasePrice() * quantity * nights;
            roomRows.add(new RoomRow(type.name(), quantity, sample.getCapacity(),
                    sample.getBasePrice(), subtotal));
        });
    }

    private void populateAddOnTable(List<String> addOns, LocalDate checkIn, LocalDate checkOut) {
        addOnRows.clear();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        for (String addOn : addOns) {
            double price = ADD_ON_PRICES.getOrDefault(addOn, 0.0);
            boolean perNight = ADD_ON_PER_NIGHT.getOrDefault(addOn, false);
            double total = perNight ? price * nights : price;
            addOnRows.add(new AddOnRow(addOn, perNight ? "Per Night" : "Per Stay", price, total));
        }
    }

    private void updateFinancials(Reservation reservation, List<RoomType> rooms,
                                  List<String> addOns, double amountPaid) {
        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckIn(), reservation.getCheckOut());

        // Calculate room subtotal
        double roomSubtotal = rooms.stream()
                .mapToDouble(room -> room.getBasePrice() * nights)
                .sum();

        // Calculate add-ons total
        double addonsTotal = addOns.stream()
                .mapToDouble(addOn -> {
                    double price = ADD_ON_PRICES.getOrDefault(addOn, 0.0);
                    return ADD_ON_PER_NIGHT.getOrDefault(addOn, false) ? price * nights : price;
                })
                .sum();

        double subtotal = roomSubtotal + addonsTotal;

        // Apply discount
        double discountPercent = reservation.getDiscountPercent();
        double discountAmount = subtotal * (discountPercent / 100.0);
        double afterDiscount = subtotal - discountAmount;

        // Calculate tax on discounted amount
        double taxRate = 0.13; // 13% tax
        double tax = afterDiscount * taxRate;

        // Grand total
        double grandTotal = afterDiscount + tax;
        double balance = Math.max(0, grandTotal - amountPaid);

        // Update labels
        roomSubtotalLabel.setText(formatCurrency(roomSubtotal));
        addonsTotalLabel.setText(formatCurrency(addonsTotal));
        subtotalLabel.setText(formatCurrency(subtotal));
        taxLabel.setText(formatCurrency(tax));

        // Show/hide discount row
        boolean hasDiscount = discountPercent > 0;
        discountRow.setVisible(hasDiscount);
        discountRow.setManaged(hasDiscount);
        discountLabel.setVisible(hasDiscount);
        discountLabel.setManaged(hasDiscount);
        if (hasDiscount) {
            discountLabel.setText(String.format("-$%.2f (%.1f%%)", discountAmount, discountPercent));
        }

        // Hide loyalty redemption for now
        loyaltyRedemptionRow.setVisible(false);
        loyaltyRedemptionRow.setManaged(false);
        loyaltyRedemptionLabel.setVisible(false);
        loyaltyRedemptionLabel.setManaged(false);

        grandTotalLabel.setText(formatCurrency(grandTotal));
        paidLabel.setText(formatCurrency(amountPaid));
        balanceLabel.setText(formatCurrency(balance));
    }

    // ─────────── ACTION HANDLERS ───────────

    private void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private void handleModify() {
        showInfo("Modify", "Modify reservation functionality");
    }

    private void handleApplyDiscount() {
        if (currentReservation == null) {
            showWarning("No Reservation", "No reservation loaded");
            return;
        }

        // Get current user and check permissions
        AdminUser currentUser = authService != null ? authService.getCurrentUser() : null;
        if (currentUser == null) {
            showWarning("Not Authorized", "You must be logged in to apply discounts");
            return;
        }

        // Determine max discount based on role
        double maxDiscount = currentUser.getRole() == AdminUser.Role.MANAGER ? 30.0 : 15.0;

        // Show dialog to enter discount
        TextInputDialog dialog = new TextInputDialog(
                String.valueOf(currentReservation.getDiscountPercent()));
        dialog.setTitle("Apply Discount");
        dialog.setHeaderText(String.format("Your role allows up to %.0f%% discount", maxDiscount));
        dialog.setContentText("Enter discount percentage:");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double percent = Double.parseDouble(input.trim());

                if (percent < 0 || percent > 100) {
                    showWarning("Invalid Discount", "Discount must be between 0 and 100");
                    return;
                }

                if (percent > maxDiscount) {
                    showWarning("Discount Limit Exceeded",
                            String.format("Your role only allows %.0f%% discount", maxDiscount));
                    return;
                }

                // Apply discount
                if (reservationService != null) {
                    reservationService.applyDiscount(currentReservation.getId(), percent);
                    currentReservation.setDiscountPercent(percent);

                    // Refresh display
                    updateFinancials(currentReservation, currentReservation.getRooms(),
                            new ArrayList<>(), 0.0);

                    showInfo("Success", String.format("%.1f%% discount applied", percent));
                }
            } catch (NumberFormatException e) {
                showWarning("Invalid Input", "Please enter a valid number");
            } catch (Exception e) {
                showError("Error", "Failed to apply discount: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Discount error", e);
            }
        });
    }

    private void handleProcessPayment() {
        showInfo("Payment", "Process payment functionality");
    }

    private void handleCheckout() {
        if (currentReservation == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Checkout");
        confirm.setContentText("Complete checkout for this reservation?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && reservationService != null) {
                try {
                    reservationService.checkOut(currentReservation.getId());
                    showInfo("Success", "Checkout completed");
                    handleBack();
                } catch (Exception e) {
                    showError("Error", "Checkout failed: " + e.getMessage());
                }
            }
        });
    }

    private void handleCancel() {
        if (currentReservation == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancellation");
        confirm.setContentText("Cancel this reservation?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && reservationService != null) {
                try {
                    reservationService.cancelReservation(currentReservation.getId());
                    showInfo("Success", "Reservation cancelled");
                    handleBack();
                } catch (Exception e) {
                    showError("Error", "Cancellation failed: " + e.getMessage());
                }
            }
        });
    }

    // ─────────── UTILITY METHODS ───────────

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    private String formatDate(LocalDate date) {
        return date != null ? DATE_FORMATTER.format(date) : "N/A";
    }

    // ─────────── TABLE ROW CLASSES ───────────

    public static class RoomRow {
        private final String roomType;
        private final int quantity;
        private final int capacity;
        private final double price;
        private final double total;

        public RoomRow(String roomType, int quantity, int capacity, double price, double total) {
            this.roomType = roomType;
            this.quantity = quantity;
            this.capacity = capacity;
            this.price = price;
            this.total = total;
        }

        public String getRoomType() { return roomType; }
        public int getQuantity() { return quantity; }
        public int getCapacity() { return capacity; }
        public double getPrice() { return price; }
        public double getTotal() { return total; }
    }

    public static class AddOnRow {
        private final String name;
        private final String pricingModel;
        private final double price;
        private final double total;

        public AddOnRow(String name, String pricingModel, double price, double total) {
            this.name = name;
            this.pricingModel = pricingModel;
            this.price = price;
            this.total = total;
        }

        public String getName() { return name; }
        public String getPricingModel() { return pricingModel; }
        public double getPrice() { return price; }
        public double getTotal() { return total; }
    }

    public static class PaymentRow {
        private final String date;
        private final String paymentType;
        private final double amount;
        private final String processedBy;
        private final String notes;

        public PaymentRow(String date, String paymentType, double amount, String processedBy, String notes) {
            this.date = date;
            this.paymentType = paymentType;
            this.amount = amount;
            this.processedBy = processedBy;
            this.notes = notes;
        }

        public String getDate() { return date; }
        public String getPaymentType() { return paymentType; }
        public double getAmount() { return amount; }
        public String getProcessedBy() { return processedBy; }
        public String getNotes() { return notes; }
    }
}