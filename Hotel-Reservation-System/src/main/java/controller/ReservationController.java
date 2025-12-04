package controller;

import app.Bootstrap;
import config.LoyaltyConfig;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import model.Guest;
import model.Reservation;
import model.RoomType;
import service.BillingContext;
import service.LoyaltyService;

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

    private Reservation currentReservation;
    private final ObservableList<RoomRow> roomRows = FXCollections.observableArrayList();
    private final ObservableList<AddOnRow> addOnRows = FXCollections.observableArrayList();
    private final ObservableList<PaymentRow> paymentRows = FXCollections.observableArrayList();

    // Static pricing map mirrors BillingContext defaults for UI breakdowns
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

    public ReservationController() {
        BillingContext context;
        LoyaltyService loyalty;
        try {
            context = Bootstrap.getBillingContext();
            loyalty = Bootstrap.getLoyaltyService();
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Fallback reservation controller initialization", ex);
            context = new BillingContext();
            loyalty = new LoyaltyService(new LoyaltyConfig());
        }

        this.billingContext = context;
        this.loyaltyService = loyalty;
    }

    public ReservationController(BillingContext billingContext, LoyaltyService loyaltyService) {
        this.billingContext = billingContext;
        this.loyaltyService = loyaltyService;
    }

    // ─────────── TOP BAR ───────────
    @FXML private Button backButton;
    @FXML private Label  titleLabel;
    @FXML private Label  reservationIdLabel;
    @FXML private Label  statusBadge;

    // ─────────── GUEST DETAILS ───────────
    @FXML private Label guestNameLabel;
    @FXML private Label guestPhoneLabel;
    @FXML private Label guestEmailLabel;

    @FXML private Label loyaltyStatusLabel;
    @FXML private Button enrollLoyaltyButton;

    @FXML private VBox   loyaltyNumberSection;
    @FXML private Label  loyaltyNumberLabel;

    @FXML private VBox   loyaltyPointsSection;
    @FXML private Label  loyaltyPointsLabel;

    // ─────────── RESERVATION DETAILS ───────────
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label nightsLabel;
    @FXML private Label guestsLabel;
    @FXML private Label bookingDateLabel;
    @FXML private Label bookedByLabel;

    // ─────────── ROOM TABLE ───────────
    @FXML private TableView<RoomRow>      roomsTable;
    @FXML private TableColumn<RoomRow, String> roomTypeCol;
    @FXML private TableColumn<RoomRow, Integer> roomQuantityCol;
    @FXML private TableColumn<RoomRow, Integer> roomCapacityCol;
    @FXML private TableColumn<RoomRow, Double> roomPriceCol;
    @FXML private TableColumn<RoomRow, Double> roomTotalCol;

    // ─────────── ADD-ON SERVICES TABLE ───────────
    @FXML private TableView<AddOnRow>      addonsTable;
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
    @FXML private TableView<PaymentRow>      paymentsTable;
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

    // ─────────── LIFECYCLE ───────────
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
        // make sure managed matches visible so hidden sections don’t take space
        if (loyaltyNumberSection != null) {
            loyaltyNumberSection.setManaged(loyaltyNumberSection.isVisible());
        }
        if (loyaltyPointsSection != null) {
            loyaltyPointsSection.setManaged(loyaltyPointsSection.isVisible());
        }
    }

    private void configureTables() {
        if (roomTypeCol != null) {
            roomTypeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
            roomQuantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            roomCapacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
            roomPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
            roomTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
            roomsTable.setItems(roomRows);
        }

        if (addonNameCol != null) {
            addonNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            addonPricingCol.setCellValueFactory(new PropertyValueFactory<>("pricingModel"));
            addonPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
            addonTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
            addonsTable.setItems(addOnRows);
        }

        if (paymentDateCol != null) {
            paymentDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            paymentTypeCol.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
            paymentAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            paymentByCol.setCellValueFactory(new PropertyValueFactory<>("processedBy"));
            paymentNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
            paymentsTable.setItems(paymentRows);
        }
    }

    private void configureButtons() {
        if (backButton != null) {
            backButton.setOnAction(event -> showInfo("Navigation", "Return to previous screen"));
        }
        if (modifyButton != null) {
            modifyButton.setOnAction(event -> showInfo("Modify", "Modify reservation workflow goes here."));
        }
        if (applyDiscountButton != null) {
            applyDiscountButton.setOnAction(event -> showInfo("Discount", "Open discount dialog."));
        }
        if (processPaymentButton != null) {
            processPaymentButton.setOnAction(event -> showInfo("Payment", "Launch payment processing."));
        }
        if (checkoutButton != null) {
            checkoutButton.setOnAction(event -> showInfo("Checkout", "Checkout reservation."));
        }
        if (cancelButton != null) {
            cancelButton.setOnAction(event -> showInfo("Cancel", "Cancel reservation and release rooms."));
        }
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
        reservation.setStatus("BOOKED");
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
                200.0, 20.0, 0.0, "Kiosk");
    }

    private void populateGuestSection(Guest guest) {
        guestNameLabel.setText(guest.getFirstName() + " " + guest.getLastName());
        guestPhoneLabel.setText(guest.getPhone());
        guestEmailLabel.setText(guest.getEmail());

        // Loyalty status (simple heuristic based on presence of email)
        boolean isMember = guest.getEmail() != null && guest.getEmail().contains("@");
        loyaltyStatusLabel.setText(isMember ? "Yes" : "No");
        enrollLoyaltyButton.setVisible(!isMember);
        enrollLoyaltyButton.setManaged(!isMember);

        int points = isMember ? loyaltyService.calculateEarnedPoints(150.0) : 0;
        loyaltyNumberSection.setVisible(isMember);
        loyaltyNumberSection.setManaged(isMember);
        loyaltyPointsSection.setVisible(isMember);
        loyaltyPointsSection.setManaged(isMember);

        if (isMember) {
            loyaltyNumberLabel.setText("GP-" + guest.getFirstName().substring(0, 1).toUpperCase() + "" + guest.getLastName().substring(0, 1).toUpperCase() + "1234");
            loyaltyPointsLabel.setText(String.valueOf(points));
        }
        syncSectionVisibility();
    }

    private void populateReservationSection(Reservation reservation, int guestCount, String bookedBy) {
        titleLabel.setText("Reservation Details");
        reservationIdLabel.setText(reservation.getId() != null ? "ID: #" + reservation.getId() : "ID: Pending");
        statusBadge.setText(reservation.getStatus());

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
        double tax = subtotal * 0.13;
        double grandTotal = subtotal - discount - loyaltyRedemption + tax;
        double balance = Math.max(0, grandTotal - amountPaid);

        roomSubtotalLabel.setText(formatCurrency(roomSubtotal));
        addonsTotalLabel.setText(formatCurrency(addonsTotal));
        subtotalLabel.setText(formatCurrency(subtotal));
        taxLabel.setText(formatCurrency(tax));

        discountRow.setVisible(discount > 0);
        discountRow.setManaged(discount > 0);
        discountLabel.setVisible(discount > 0);
        discountLabel.setManaged(discount > 0);
        discountLabel.setText("-" + formatCurrency(discount));

        loyaltyRedemptionRow.setVisible(loyaltyRedemption > 0);
        loyaltyRedemptionRow.setManaged(loyaltyRedemption > 0);
        loyaltyRedemptionLabel.setVisible(loyaltyRedemption > 0);
        loyaltyRedemptionLabel.setManaged(loyaltyRedemption > 0);
        loyaltyRedemptionLabel.setText("-" + formatCurrency(loyaltyRedemption));

        grandTotalLabel.setText(formatCurrency(grandTotal));
        paidLabel.setText(formatCurrency(amountPaid));
        balanceLabel.setText(formatCurrency(balance));
    }

    private void showInfo(String title, String message) {
        LOGGER.info(() -> title + ": " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
        private final SimpleStringProperty roomType;
        private final SimpleIntegerProperty quantity;
        private final SimpleIntegerProperty capacity;
        private final SimpleDoubleProperty price;
        private final SimpleDoubleProperty total;

        public RoomRow(String roomType, int quantity, int capacity, double price, double total) {
            this.roomType = new SimpleStringProperty(roomType);
            this.quantity = new SimpleIntegerProperty(quantity);
            this.capacity = new SimpleIntegerProperty(capacity);
            this.price = new SimpleDoubleProperty(price);
            this.total = new SimpleDoubleProperty(total);
        }

        public String getRoomType() { return roomType.get(); }
        public int getQuantity() { return quantity.get(); }
        public int getCapacity() { return capacity.get(); }
        public double getPrice() { return price.get(); }
        public double getTotal() { return total.get(); }
    }

    public static class AddOnRow {
        private final SimpleStringProperty name;
        private final SimpleStringProperty pricingModel;
        private final SimpleDoubleProperty price;
        private final SimpleDoubleProperty total;

        public AddOnRow(String name, String pricingModel, double price, double total) {
            this.name = new SimpleStringProperty(name);
            this.pricingModel = new SimpleStringProperty(pricingModel);
            this.price = new SimpleDoubleProperty(price);
            this.total = new SimpleDoubleProperty(total);
        }

        public String getName() { return name.get(); }
        public String getPricingModel() { return pricingModel.get(); }
        public double getPrice() { return price.get(); }
        public double getTotal() { return total.get(); }
    }

    public static class PaymentRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty paymentType;
        private final SimpleDoubleProperty amount;
        private final SimpleStringProperty processedBy;
        private final SimpleStringProperty notes;

        public PaymentRow(String date, String paymentType, double amount, String processedBy, String notes) {
            this.date = new SimpleStringProperty(date);
            this.paymentType = new SimpleStringProperty(paymentType);
            this.amount = new SimpleDoubleProperty(amount);
            this.processedBy = new SimpleStringProperty(processedBy);
            this.notes = new SimpleStringProperty(notes);
        }

        public String getDate() { return date.get(); }
        public String getPaymentType() { return paymentType.get(); }
        public double getAmount() { return amount.get(); }
        public String getProcessedBy() { return processedBy.get(); }
        public String getNotes() { return notes.get(); }
    }
}
