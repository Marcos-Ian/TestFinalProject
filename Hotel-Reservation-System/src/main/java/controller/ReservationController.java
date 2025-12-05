package controller;

import app.Bootstrap;
import config.LoyaltyConfig;
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
import model.ReservationAddOn;
import model.RoomType;
import security.AdminUser;
import security.AuthenticationService;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReservationController {
    private static final Logger LOGGER = Logger.getLogger(ReservationController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final BillingContext billingContext;
    private final LoyaltyService loyaltyService;
    private final ReservationService reservationService;
    private final AuthenticationService authService;

    private Reservation currentReservation;
    private final ObservableList<RoomRow> roomRows = FXCollections.observableArrayList();
    private final ObservableList<AddOnRow> addOnRows = FXCollections.observableArrayList();
    private final ObservableList<PaymentRow> paymentRows = FXCollections.observableArrayList();

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
    @FXML private VBox loyaltyNumberSection;
    @FXML private Label loyaltyNumberLabel;
    @FXML private VBox loyaltyPointsSection;
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

    public ReservationController() {
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

    public ReservationController(BillingContext billingContext, LoyaltyService loyaltyService) {
        this.billingContext = billingContext;
        this.loyaltyService = loyaltyService;
        this.reservationService = null;
        this.authService = null;
    }

    @FXML
    public void initialize() {
        configureTables();
        configureButtons();
        syncSectionVisibility();

        if (currentReservation == null) {
            populateSampleData();
        }
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
        if (roomTypeCol != null) {
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

        if (addonNameCol != null) {
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

        if (paymentDateCol != null) {
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
            modifyButton.setOnAction(event -> showInfo("Modify", "Modify reservation functionality"));
        }
        if (applyDiscountButton != null) {
            applyDiscountButton.setOnAction(event -> handleApplyDiscount());
        }
        if (processPaymentButton != null) {
            processPaymentButton.setOnAction(event -> showInfo("Payment", "Process payment functionality"));
        }
        if (checkoutButton != null) {
            checkoutButton.setOnAction(event -> handleCheckout());
        }
        if (cancelButton != null) {
            cancelButton.setOnAction(event -> handleCancel());
        }
    }

    /**
     * Convenience method to populate the details view using only the reservation entity.
     */
    public void displayReservation(Reservation reservation) {
        List<RoomType> rooms = reservation.getRooms() != null ?
                new ArrayList<>(reservation.getRooms()) : new ArrayList<>();

        List<String> addOns = new ArrayList<>();
        if (reservation.getAddOns() != null) {
            for (ReservationAddOn addOn : reservation.getAddOns()) {
                addOns.add(addOn.getAddOnName());
            }
        }

        List<PaymentRow> payments = new ArrayList<>();

        int totalGuests = rooms.stream()
                .mapToInt(RoomType::getCapacity)
                .sum();
        if (totalGuests == 0) {
            totalGuests = 2;
        }

        double amountPaid = 0.0;
        double discount = reservation.getDiscountPercent() != null ?
                reservation.getDiscountPercent() : 0.0;
        double loyaltyRedemption = 0.0;
        String bookedBy = "Kiosk";

        displayReservation(reservation, rooms, addOns, payments, totalGuests,
                amountPaid, discount, loyaltyRedemption, bookedBy);
    }

    public void displayReservation(Reservation reservation, List<RoomType> rooms,
                                   List<String> addOns, List<PaymentRow> payments,
                                   int totalGuests, double amountPaid,
                                   double discount, double loyaltyRedemption,
                                   String bookedBy) {
        this.currentReservation = reservation;
        this.paymentRows.setAll(payments);

        populateGuestSection(reservation.getGuest());
        populateReservationSection(reservation, totalGuests, bookedBy);
        populateRoomTable(rooms, reservation.getCheckIn(), reservation.getCheckOut());
        populateAddOnTable(addOns, reservation.getCheckIn(), reservation.getCheckOut());
        updateFinancials(reservation.getCheckIn(), reservation.getCheckOut(), rooms, addOns,
                amountPaid, discount, loyaltyRedemption);
    }

    private void populateSampleData() {
        Guest guest = new Guest();
        guest.setFirstName("Alex");
        guest.setLastName("Jordan");
        guest.setEmail("alex.jordan@example.com");
        guest.setPhone("5551234567");

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setCheckIn(LocalDate.now().plusDays(3));
        reservation.setCheckOut(LocalDate.now().plusDays(6));
        reservation.setStatus(model.ReservationStatus.BOOKED);
        reservation.setDiscountPercent(0.0);
        this.currentReservation = reservation;

        List<RoomType> rooms = new ArrayList<>();
        RoomType deluxe = new RoomType();
        deluxe.setType(RoomType.Type.DELUXE);
        deluxe.setBasePrice(220.0);
        deluxe.setCapacity(2);
        rooms.add(deluxe);

        RoomType doubleRoom = new RoomType();
        doubleRoom.setType(RoomType.Type.DOUBLE);
        doubleRoom.setBasePrice(180.0);
        doubleRoom.setCapacity(4);
        rooms.add(doubleRoom);

        List<String> addOns = List.of("Breakfast", "WiFi");
        List<PaymentRow> payments = new ArrayList<>();
        payments.add(new PaymentRow("Today", "Card", 200.0, "Front Desk", "Deposit"));

        displayReservation(reservation, rooms, addOns, payments, 3,
                200.0, 0.0, 0.0, "Kiosk");
    }

    private void populateGuestSection(Guest guest) {
        guestNameLabel.setText(guest.getFirstName() + " " + guest.getLastName());
        guestPhoneLabel.setText(guest.getPhone());
        guestEmailLabel.setText(guest.getEmail());

        boolean isMember = guest.getEmail() != null && guest.getEmail().contains("@");
        loyaltyStatusLabel.setText(isMember ? "Yes" : "No");

        if (enrollLoyaltyButton != null) {
            enrollLoyaltyButton.setVisible(!isMember);
            enrollLoyaltyButton.setManaged(!isMember);
        }

        int points = isMember ? loyaltyService.calculateEarnedPoints(150.0) : 0;
        loyaltyNumberSection.setVisible(isMember);
        loyaltyNumberSection.setManaged(isMember);
        loyaltyPointsSection.setVisible(isMember);
        loyaltyPointsSection.setManaged(isMember);

        if (isMember) {
            loyaltyNumberLabel.setText("GP-" + guest.getFirstName().substring(0, 1).toUpperCase() +
                    "" + guest.getLastName().substring(0, 1).toUpperCase() + "1234");
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
            roomRows.add(new RoomRow(type.name(), quantity, sample.getCapacity(), sample.getBasePrice(), subtotal));
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

    private void updateFinancials(LocalDate checkIn, LocalDate checkOut, List<RoomType> rooms,
                                  List<String> addOns, double amountPaid,
                                  double discount, double loyaltyRedemption) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double roomSubtotal = rooms.stream()
                .mapToDouble(room -> room.getBasePrice() * nights)
                .sum();

        double addonsTotal = addOns.stream()
                .mapToDouble(addOn -> {
                    double price = ADD_ON_PRICES.getOrDefault(addOn, 0.0);
                    return ADD_ON_PER_NIGHT.getOrDefault(addOn, false) ? price * nights : price;
                })
                .sum();

        double subtotal = roomSubtotal + addonsTotal;

        // Apply discount if exists
        double discountPercent = currentReservation != null ? currentReservation.getDiscountPercent() : 0.0;
        double discountAmount = subtotal * (discountPercent / 100.0);
        double afterDiscount = subtotal - discountAmount;

        // Calculate tax on discounted amount
        double tax = afterDiscount * 0.13;

        double grandTotal = afterDiscount - loyaltyRedemption + tax;
        double balance = Math.max(0, grandTotal - amountPaid);

        roomSubtotalLabel.setText(formatCurrency(roomSubtotal));
        addonsTotalLabel.setText(formatCurrency(addonsTotal));
        subtotalLabel.setText(formatCurrency(subtotal));
        taxLabel.setText(formatCurrency(tax));

        // Show/hide discount
        boolean hasDiscount = discountPercent > 0;
        discountRow.setVisible(hasDiscount);
        discountRow.setManaged(hasDiscount);
        discountLabel.setVisible(hasDiscount);
        discountLabel.setManaged(hasDiscount);
        if (hasDiscount) {
            discountLabel.setText(String.format("-$%.2f (%.1f%%)", discountAmount, discountPercent));
        }

        loyaltyRedemptionRow.setVisible(loyaltyRedemption > 0);
        loyaltyRedemptionRow.setManaged(loyaltyRedemption > 0);
        loyaltyRedemptionLabel.setVisible(loyaltyRedemption > 0);
        loyaltyRedemptionLabel.setManaged(loyaltyRedemption > 0);
        loyaltyRedemptionLabel.setText("-" + formatCurrency(loyaltyRedemption));

        grandTotalLabel.setText(formatCurrency(grandTotal));
        paidLabel.setText(formatCurrency(amountPaid));
        balanceLabel.setText(formatCurrency(balance));
    }

    // ─────────── ACTION HANDLERS ───────────

    private void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private void handleApplyDiscount() {
        if (currentReservation == null) {
            showWarning("No Reservation", "No reservation loaded");
            return;
        }

        AdminUser currentUser = authService != null ? authService.getCurrentUser() : null;
        if (currentUser == null) {
            showWarning("Not Authorized", "You must be logged in to apply discounts");
            return;
        }

        double maxDiscount = currentUser.getRole() == AdminUser.Role.MANAGER ? 30.0 : 15.0;

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

                if (reservationService != null) {
                    reservationService.applyDiscount(currentReservation.getId(), percent);
                    currentReservation.setDiscountPercent(percent);

                    // Refresh display
                    updateFinancials(currentReservation.getCheckIn(), currentReservation.getCheckOut(),
                            currentReservation.getRooms(), new ArrayList<>(), 0.0, 0.0, 0.0);

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
        return DATE_FORMATTER.format(date);
    }

    // Table row DTOs
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