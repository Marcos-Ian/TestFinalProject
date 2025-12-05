package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import model.Guest;
import model.Reservation;
import model.RoomType;
import model.ReservationStatus;
import security.AdminUser;
import security.AuthenticationService;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import service.RoomService;
import util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller for the admin module.
 * Handles authentication, dashboard, reservations, payments, checkout, and reporting.
 */
public class AdminController {
    private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());

    // Services (injected via constructor or Bootstrap)
    private final ReservationService reservationService;
    private final RoomService roomService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;
    private final AuthenticationService authService;
    private final PaymentService paymentService;
    private final WaitlistService waitlistService;
    private final FeedbackService feedbackService;

    // Current logged-in admin
    private AdminUser currentAdmin;

    // ========== FXML Controls - Login Screen ==========
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button loginButton;
    @FXML private Label loginErrorLabel;

    // ========== FXML Controls - Dashboard ==========
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Button logoutButton;
    @FXML private TabPane mainTabPane;

    // Search Controls
    @FXML private TextField searchNameField;
    @FXML private TextField searchPhoneField;
    @FXML private DatePicker searchStartDatePicker;
    @FXML private DatePicker searchEndDatePicker;
    @FXML private ComboBox<String> searchStatusCombo;
    @FXML private Button searchButton;
    @FXML private Button clearSearchButton;

    // Reservation Table
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Long> colReservationId;
    @FXML private TableColumn<Reservation, String> colGuestName;
    @FXML private TableColumn<Reservation, String> colGuestPhone;
    @FXML private TableColumn<Reservation, LocalDate> colCheckIn;
    @FXML private TableColumn<Reservation, LocalDate> colCheckOut;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private Button viewDetailsButton;
    @FXML private Button createReservationButton;
    @FXML private Button modifyReservationButton;
    @FXML private Button cancelReservationButton;

    // ========== FXML Controls - Payments Tab ==========
    @FXML private TextField paymentReservationIdField;
    @FXML private Button loadReservationButton;
    @FXML private Label paymentGuestLabel;
    @FXML private Label paymentTotalLabel;
    @FXML private Label paymentPaidLabel;
    @FXML private Label paymentBalanceLabel;
    @FXML private TextField paymentAmountField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextField loyaltyPointsField;
    @FXML private Button processPaymentButton;
    @FXML private Button processRefundButton;
    @FXML private TableView<PaymentRecord> paymentHistoryTable;

    // ========== FXML Controls - Checkout Tab ==========
    @FXML private TextField checkoutReservationIdField;
    @FXML private Button loadCheckoutButton;
    @FXML private Label checkoutGuestLabel;
    @FXML private Label checkoutRoomsLabel;
    @FXML private Label checkoutTotalLabel;
    @FXML private Label checkoutBalanceLabel;
    @FXML private Button generateBillButton;
    @FXML private Button completeCheckoutButton;
    @FXML private TextArea billTextArea;

    // ========== FXML Controls - Waitlist Tab ==========
    @FXML private TableView<WaitlistEntry> waitlistTable;
    @FXML private TableColumn<WaitlistEntry, String> colWaitlistGuest;
    @FXML private TableColumn<WaitlistEntry, String> colWaitlistRoomType;
    @FXML private TableColumn<WaitlistEntry, LocalDate> colWaitlistDate;
    @FXML private Button addToWaitlistButton;
    @FXML private Button convertToReservationButton;
    @FXML private Button removeFromWaitlistButton;

    // ========== FXML Controls - Loyalty Tab ==========
    @FXML private TextField loyaltyGuestSearchField;
    @FXML private Button searchLoyaltyButton;
    @FXML private Label loyaltyGuestNameLabel;
    @FXML private Label loyaltyPointsBalanceLabel;
    @FXML private Button enrollGuestButton;
    @FXML private TableView<LoyaltyTransaction> loyaltyHistoryTable;

    // ========== FXML Controls - Feedback Tab ==========
    @FXML private TableView<FeedbackEntry> feedbackTable;
    @FXML private TableColumn<FeedbackEntry, Long> colFeedbackReservationId;
    @FXML private TableColumn<FeedbackEntry, String> colFeedbackGuest;
    @FXML private TableColumn<FeedbackEntry, Integer> colFeedbackRating;
    @FXML private TableColumn<FeedbackEntry, String> colFeedbackComment;
    @FXML private TableColumn<FeedbackEntry, LocalDate> colFeedbackDate;
    @FXML private ComboBox<Integer> filterRatingCombo;
    @FXML private DatePicker filterDatePicker;
    @FXML private Button filterFeedbackButton;
    @FXML private Button exportFeedbackButton;

    // ========== FXML Controls - Reports Tab ==========
    @FXML private TabPane reportsTabPane;
    @FXML private DatePicker revenueStartDate;
    @FXML private DatePicker revenueEndDate;
    @FXML private ComboBox<String> revenueTypeCombo;
    @FXML private Button generateRevenueReportButton;
    @FXML private TableView<RevenueReport> revenueReportTable;
    @FXML private Button exportRevenueButton;

    @FXML private DatePicker occupancyStartDate;
    @FXML private DatePicker occupancyEndDate;
    @FXML private ComboBox<String> occupancyTypeCombo;
    @FXML private Button generateOccupancyReportButton;
    @FXML private TableView<OccupancyReport> occupancyReportTable;
    @FXML private Button exportOccupancyButton;

    @FXML private TableView<ActivityLog> activityLogTable;
    @FXML private Button loadActivityLogsButton;
    @FXML private Button exportActivityLogsButton;

    // Observable lists for tables
    private ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private Reservation currentReservation;

    /**
     * Constructor with dependency injection
     */
    public AdminController() {
        // Get services from Bootstrap (DI container)
        this.reservationService = Bootstrap.getReservationService();
        this.roomService = Bootstrap.getRoomService();
        this.loyaltyService = Bootstrap.getLoyaltyService();
        this.billingContext = Bootstrap.getBillingContext();
        this.authService = Bootstrap.getAuthenticationService();
        this.paymentService = new PaymentService();
        this.waitlistService = new WaitlistService();
        this.feedbackService = Bootstrap.getFeedbackService();

        LOGGER.info("AdminController initialized");
    }

    /**
     * Alternative constructor for testing or direct injection
     */
    public AdminController(ReservationService reservationService, RoomService roomService,
                           LoyaltyService loyaltyService, BillingContext billingContext,
                           AuthenticationService authService, PaymentService paymentService,
                           WaitlistService waitlistService, FeedbackService feedbackService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.loyaltyService = loyaltyService;
        this.billingContext = billingContext;
        this.authService = authService;
        this.paymentService = paymentService;
        this.waitlistService = waitlistService;
        this.feedbackService = feedbackService;

        LOGGER.info("AdminController initialized with injected services");
    }

    /**
     * Initialize method called after FXML is loaded
     */
    @FXML
    public void initialize() {
        LOGGER.info("Initializing AdminController UI");
        setupLoginScreen();
    }

    // ============================================================================
    // AUTHENTICATION
    // ============================================================================

    /**
     * Set up login screen
     */
    private void setupLoginScreen() {
        if (loginButton != null) {
            loginButton.setOnAction(event -> handleLogin());
            loginPasswordField.setOnAction(event -> handleLogin());
        }
    }

    /**
     * Handle login button click
     */
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showLoginError("Please enter username and password");
            return;
        }

        LOGGER.info("Login attempt for user: " + username);

        try {
            Optional<AdminUser> adminOpt = authService.authenticate(username, password);

            if (adminOpt.isPresent()) {
                currentAdmin = adminOpt.get();
                LOGGER.info(String.format("Login successful: %s (%s)",
                        currentAdmin.getUsername(), currentAdmin.getRole()));

                // Log authentication event
                logActivity("LOGIN", "User", currentAdmin.getUsername(),
                        "User logged in successfully");

                onLoginSuccess();
            } else {
                showLoginError("Invalid username or password");
                LOGGER.warning("Login failed for user: " + username);

                // Log failed login
                logActivity("LOGIN_FAILED", "User", username, "Invalid credentials");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login error", e);
            showLoginError("System error. Please try again.");
        }
    }

    /**
     * Show login error message
     */
    private void showLoginError(String message) {
        if (loginErrorLabel != null) {
            loginErrorLabel.setText(message);
            loginErrorLabel.setVisible(true);
        }
    }

    /**
     * Handle successful login - switch to dashboard
     */
    private void onLoginSuccess() {
        try {
            // Hide login screen, show dashboard
            // This would typically involve loading a new FXML or switching scenes
            setupDashboard();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard", e);
            showError("System Error", "Failed to load dashboard");
        }
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout() {
        if (currentAdmin != null) {
            LOGGER.info("User logging out: " + currentAdmin.getUsername());
            logActivity("LOGOUT", "User", currentAdmin.getUsername(), "User logged out");
            currentAdmin = null;
        }

        // Return to login screen
        // This would involve switching back to login FXML
    }

    // ============================================================================
    // DASHBOARD & SEARCH
    // ============================================================================

    /**
     * Set up dashboard after login
     */
    private void setupDashboard() {
        // Set welcome message
        if (welcomeLabel != null && currentAdmin != null) {
            welcomeLabel.setText("Welcome, " + currentAdmin.getUsername());
            roleLabel.setText("Role: " + currentAdmin.getRole());
        }

        // Set up search controls
        setupSearchControls();

        // Set up reservation table
        setupReservationTable();

        // Set up other tabs
        setupPaymentsTab();
        setupCheckoutTab();
        setupWaitlistTab();
        setupLoyaltyTab();
        setupFeedbackTab();
        setupReportsTab();

        // Load initial data
        loadAllReservations();
    }

    /**
     * Set up search controls
     */
    private void setupSearchControls() {
        // Status combo box
        if (searchStatusCombo != null) {
            searchStatusCombo.setItems(FXCollections.observableArrayList(
                    "All", "BOOKED", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"
            ));
            searchStatusCombo.setValue("All");
        }

        // Search button
        if (searchButton != null) {
            searchButton.setOnAction(event -> handleSearch());
        }

        // Clear search button
        if (clearSearchButton != null) {
            clearSearchButton.setOnAction(event -> handleClearSearch());
        }

        // Create reservation button
        if (createReservationButton != null) {
            createReservationButton.setOnAction(event -> handleCreateReservation());
        }

        // View details button
        if (viewDetailsButton != null) {
            viewDetailsButton.setOnAction(event -> handleViewDetails());
        }

        // Modify reservation button
        if (modifyReservationButton != null) {
            modifyReservationButton.setOnAction(event -> handleModifyReservation());
        }

        // Cancel reservation button
        if (cancelReservationButton != null) {
            cancelReservationButton.setOnAction(event -> handleCancelReservation());
        }
    }

    /**
     * Set up reservation table columns
     */
    private void setupReservationTable() {
        if (reservationTable == null) return;

        colReservationId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colGuestName.setCellValueFactory(cellData -> {
            Guest guest = cellData.getValue().getGuest();
            String name = guest.getFirstName() + " " + guest.getLastName();
            return new SimpleStringProperty(name);
        });

        colGuestPhone.setCellValueFactory(cellData -> {
            Guest guest = cellData.getValue().getGuest();
            return new SimpleStringProperty(guest.getPhone());
        });

        colCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        colCheckOut.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        reservationTable.setItems(reservations);

        // Enable row selection
        reservationTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        currentReservation = newSelection;
                    }
                }
        );
    }

    /**
     * Handle search button click
     */
    private void handleSearch() {
        LOGGER.info("Searching reservations");

        String name = searchNameField.getText().trim();
        String phone = searchPhoneField.getText().trim();
        LocalDate startDate = searchStartDatePicker.getValue();
        LocalDate endDate = searchEndDatePicker.getValue();
        String status = searchStatusCombo.getValue();
        ReservationStatus statusFilter = null;
        if (status != null && !status.equalsIgnoreCase("All")) {
            statusFilter = ReservationStatus.valueOf(status);
        }

        List<Reservation> results = reservationService.searchReservations(
                name, phone, "", startDate, endDate, statusFilter);

        reservations.setAll(results);
        LOGGER.info("Search returned " + results.size() + " results");

        logActivity("SEARCH", "Reservation", null,
                String.format("Searched reservations with filters: name=%s, phone=%s, status=%s",
                        name, phone, status));
    }

    /**
     * Handle clear search
     */
    private void handleClearSearch() {
        searchNameField.clear();
        searchPhoneField.clear();
        searchStartDatePicker.setValue(null);
        searchEndDatePicker.setValue(null);
        searchStatusCombo.setValue("All");

        loadAllReservations();
    }

    /**
     * Load all reservations
     */
    private void loadAllReservations() {
        try {
            List<Reservation> allReservations = reservationService.findAll();
            reservations.setAll(allReservations);
            LOGGER.info("Loaded " + allReservations.size() + " reservations");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load reservations", e);
            showError("Load Error", "Failed to load reservations");
        }
    }

    // ============================================================================
    // RESERVATION MANAGEMENT
    // ============================================================================

    /**
     * Handle create new reservation (via phone booking)
     */
    private void handleCreateReservation() {
        LOGGER.info("Creating new reservation");

        try {
            // Show reservation creation dialog
            ReservationDialog dialog = new ReservationDialog(null, roomService, billingContext);
            Optional<ReservationData> result = dialog.showAndWait();

            if (result.isPresent()) {
                ReservationData data = result.get();

                // Create guest
                Guest guest = new Guest();
                guest.setFirstName(data.getFirstName());
                guest.setLastName(data.getLastName());
                guest.setPhone(data.getPhone());
                guest.setEmail(data.getEmail());

                // Create reservation
                Reservation reservation = new Reservation();
                reservation.setGuest(guest);
                reservation.setCheckIn(data.getCheckIn());
                reservation.setCheckOut(data.getCheckOut());
                reservation.setStatus(ReservationStatus.BOOKED);

                // Save reservation
                Reservation saved = reservationService.createReservation(
                        reservation, data.getRooms(), data.getAddOns());

                // Refresh table
                loadAllReservations();

                showInfo("Success", "Reservation created: #" + saved.getId());
                logActivity("CREATE", "Reservation", saved.getId().toString(),
                        "Reservation created via phone booking");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create reservation", e);
            showError("Creation Error", "Failed to create reservation: " + e.getMessage());
        }
    }

    /**
     * Handle view reservation details
     */
    private void handleViewDetails() {
        if (currentReservation == null) {
            showWarning("No Selection", "Please select a reservation to view");
            return;
        }

        LOGGER.info("Viewing reservation details: " + currentReservation.getId());

        try {
            // Show details dialog
            ReservationDetailsDialog dialog = new ReservationDetailsDialog(currentReservation);
            dialog.showAndWait();

            logActivity("VIEW", "Reservation", currentReservation.getId().toString(),
                    "Viewed reservation details");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to view details", e);
            showError("View Error", "Failed to load reservation details");
        }
    }

    /**
     * Handle modify reservation
     */
    private void handleModifyReservation() {
        if (currentReservation == null) {
            showWarning("No Selection", "Please select a reservation to modify");
            return;
        }

        LOGGER.info("Modifying reservation: " + currentReservation.getId());

        try {
            // Show modification dialog
            ReservationDialog dialog = new ReservationDialog(
                    currentReservation, roomService, billingContext);
            Optional<ReservationData> result = dialog.showAndWait();

            if (result.isPresent()) {
                ReservationData data = result.get();

                // Update reservation
                currentReservation.setCheckIn(data.getCheckIn());
                currentReservation.setCheckOut(data.getCheckOut());
                // Update other fields...

                reservationService.updateReservation(currentReservation);

                // Refresh table
                loadAllReservations();

                showInfo("Success", "Reservation updated");
                logActivity("MODIFY", "Reservation", currentReservation.getId().toString(),
                        "Reservation modified");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to modify reservation", e);
            showError("Modification Error", "Failed to modify reservation: " + e.getMessage());
        }
    }

    /**
     * Handle cancel reservation
     */
    private void handleCancelReservation() {
        if (currentReservation == null) {
            showWarning("No Selection", "Please select a reservation to cancel");
            return;
        }

        // Confirm cancellation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancellation");
        confirm.setHeaderText("Cancel Reservation #" + currentReservation.getId());
        confirm.setContentText("Are you sure you want to cancel this reservation?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                LOGGER.info("Cancelling reservation: " + currentReservation.getId());

                reservationService.cancelReservation(currentReservation.getId());

                // Refresh table
                loadAllReservations();

                showInfo("Success", "Reservation cancelled");
                logActivity("CANCEL", "Reservation", currentReservation.getId().toString(),
                        "Reservation cancelled");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to cancel reservation", e);
                showError("Cancellation Error", "Failed to cancel reservation: " + e.getMessage());
            }
        }
    }

    // ============================================================================
    // PAYMENTS
    // ============================================================================

    /**
     * Set up payments tab
     */
    private void setupPaymentsTab() {
        if (paymentMethodCombo != null) {
            paymentMethodCombo.setItems(FXCollections.observableArrayList(
                    "Cash", "Card", "Loyalty Points"
            ));
            paymentMethodCombo.setValue("Cash");
        }

        if (loadReservationButton != null) {
            loadReservationButton.setOnAction(event -> handleLoadReservationForPayment());
        }

        if (processPaymentButton != null) {
            processPaymentButton.setOnAction(event -> handleProcessPayment());
        }

        if (processRefundButton != null) {
            processRefundButton.setOnAction(event -> handleProcessRefund());
        }
    }

    /**
     * Load reservation for payment
     */
    private void handleLoadReservationForPayment() {
        String idText = paymentReservationIdField.getText().trim();

        if (idText.isEmpty()) {
            showWarning("Input Required", "Please enter a reservation ID");
            return;
        }

        try {
            Long id = Long.parseLong(idText);
            Optional<Reservation> reservationOpt = reservationService.findById(id);

            if (reservationOpt.isPresent()) {
                currentReservation = reservationOpt.get();
                displayPaymentInformation();
                LOGGER.info("Loaded reservation for payment: " + id);
            } else {
                showWarning("Not Found", "Reservation not found");
            }
        } catch (NumberFormatException e) {
            showWarning("Invalid Input", "Please enter a valid reservation ID");
        }
    }

    /**
     * Display payment information
     */
    private void displayPaymentInformation() {
        Guest guest = currentReservation.getGuest();
        paymentGuestLabel.setText(guest.getFirstName() + " " + guest.getLastName());

        // TODO: Get actual amounts from payment service
        double total = 500.0;
        double paid = 200.0;
        double balance = total - paid;

        paymentTotalLabel.setText(String.format("$%.2f", total));
        paymentPaidLabel.setText(String.format("$%.2f", paid));
        paymentBalanceLabel.setText(String.format("$%.2f", balance));

        // Load payment history
        // TODO: Load actual payment history
    }

    /**
     * Process payment
     */
    private void handleProcessPayment() {
        if (currentReservation == null) {
            showWarning("No Reservation", "Please load a reservation first");
            return;
        }

        String amountText = paymentAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showWarning("Input Required", "Please enter payment amount");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            String method = paymentMethodCombo.getValue();

            if (amount <= 0) {
                showWarning("Invalid Amount", "Payment amount must be positive");
                return;
            }

            // Process payment based on method
            if ("Loyalty Points".equals(method)) {
                // Handle loyalty points redemption
                String pointsText = loyaltyPointsField.getText().trim();
                if (pointsText.isEmpty()) {
                    showWarning("Input Required", "Please enter loyalty points");
                    return;
                }
                int points = Integer.parseInt(pointsText);
                // TODO: Process loyalty payment
            } else {
                // Process cash/card payment
                paymentService.processPayment(currentReservation.getId(), amount, method);
            }

            showInfo("Success", String.format("Payment of $%.2f processed", amount));
            logActivity("PAYMENT", "Reservation", currentReservation.getId().toString(),
                    String.format("Payment processed: $%.2f via %s", amount, method));

            // Refresh payment info
            displayPaymentInformation();
            paymentAmountField.clear();

        } catch (NumberFormatException e) {
            showWarning("Invalid Input", "Please enter a valid amount");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Payment processing failed", e);
            showError("Payment Error", "Failed to process payment: " + e.getMessage());
        }
    }

    /**
     * Process refund
     */
    private void handleProcessRefund() {
        if (currentReservation == null) {
            showWarning("No Reservation", "Please load a reservation first");
            return;
        }

        String amountText = paymentAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showWarning("Input Required", "Please enter refund amount");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                showWarning("Invalid Amount", "Refund amount must be positive");
                return;
            }

            // Confirm refund
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Refund");
            confirm.setContentText(String.format("Process refund of $%.2f?", amount));

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                paymentService.processRefund(currentReservation.getId(), amount);

                showInfo("Success", String.format("Refund of $%.2f processed", amount));
                logActivity("REFUND", "Reservation", currentReservation.getId().toString(),
                        String.format("Refund processed: $%.2f", amount));

                // Refresh payment info
                displayPaymentInformation();
                paymentAmountField.clear();
            }

        } catch (NumberFormatException e) {
            showWarning("Invalid Input", "Please enter a valid amount");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Refund processing failed", e);
            showError("Refund Error", "Failed to process refund: " + e.getMessage());
        }
    }

    // ============================================================================
    // CHECKOUT
    // ============================================================================

    /**
     * Set up checkout tab
     */
    private void setupCheckoutTab() {
        if (loadCheckoutButton != null) {
            loadCheckoutButton.setOnAction(event -> handleLoadCheckout());
        }

        if (generateBillButton != null) {
            generateBillButton.setOnAction(event -> handleGenerateBill());
        }

        if (completeCheckoutButton != null) {
            completeCheckoutButton.setOnAction(event -> handleCompleteCheckout());
        }
    }

    /**
     * Load reservation for checkout
     */
    private void handleLoadCheckout() {
        String idText = checkoutReservationIdField.getText().trim();

        if (idText.isEmpty()) {
            showWarning("Input Required", "Please enter a reservation ID");
            return;
        }

        try {
            Long id = Long.parseLong(idText);
            Optional<Reservation> reservationOpt = reservationService.findById(id);

            if (reservationOpt.isPresent()) {
                currentReservation = reservationOpt.get();
                displayCheckoutInformation();
                LOGGER.info("Loaded reservation for checkout: " + id);
            } else {
                showWarning("Not Found", "Reservation not found");
            }
        } catch (NumberFormatException e) {
            showWarning("Invalid Input", "Please enter a valid reservation ID");
        }
    }

    /**
     * Display checkout information
     */
    private void displayCheckoutInformation() {
        Guest guest = currentReservation.getGuest();
        checkoutGuestLabel.setText(guest.getFirstName() + " " + guest.getLastName());

        // TODO: Get actual room info and amounts
        checkoutRoomsLabel.setText("2 rooms");
        checkoutTotalLabel.setText("$500.00");
        checkoutBalanceLabel.setText("$0.00");
    }

    /**
     * Generate final bill
     */
    private void handleGenerateBill() {
        if (currentReservation == null) {
            showWarning("No Reservation", "Please load a reservation first");
            return;
        }

        try {
            String bill = generateBillText();
            billTextArea.setText(bill);
            LOGGER.info("Bill generated for reservation: " + currentReservation.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate bill", e);
            showError("Bill Error", "Failed to generate bill");
        }
    }

    /**
     * Generate bill text
     */
    private String generateBillText() {
        Guest guest = currentReservation.getGuest();

        StringBuilder bill = new StringBuilder();
        bill.append("===============================================\n");
        bill.append("         GRAND PLAZA HOTEL - FINAL BILL       \n");
        bill.append("===============================================\n\n");
        bill.append(String.format("Reservation ID: %d\n", currentReservation.getId()));
        bill.append(String.format("Guest: %s %s\n", guest.getFirstName(), guest.getLastName()));
        bill.append(String.format("Phone: %s\n", guest.getPhone()));
        bill.append(String.format("Email: %s\n\n", guest.getEmail()));
        bill.append(String.format("Check-in: %s\n", currentReservation.getCheckIn()));
        bill.append(String.format("Check-out: %s\n\n", currentReservation.getCheckOut()));
        bill.append("-----------------------------------------------\n");
        bill.append("CHARGES\n");
        bill.append("-----------------------------------------------\n");
// TODO: Add actual charges from billing service
        bill.append(String.format("Room Charges:          $%.2f\n", 400.0));
        bill.append(String.format("Add-on Services:       $%.2f\n", 50.0));
        bill.append(String.format("Subtotal:              $%.2f\n", 450.0));
        bill.append(String.format("Tax (10%%):             $%.2f\n", 45.0));
        bill.append("-----------------------------------------------\n");
        bill.append(String.format("TOTAL:                 $%.2f\n", 495.0));
        bill.append(String.format("Paid:                  $%.2f\n", 495.0));
        bill.append(String.format("Balance Due:           $%.2f\n", 0.0));
        bill.append("-----------------------------------------------\n\n");
        bill.append("Thank you for staying with Grand Plaza Hotel!\n");
        bill.append("We hope to see you again soon.\n");
        bill.append("===============================================\n");
        return bill.toString();
    }

    /**
     * Complete checkout
     */
    private void handleCompleteCheckout() {
        if (currentReservation == null) {
            showWarning("No Reservation", "Please load a reservation first");
            return;
        }

        // Check if balance is settled
        // TODO: Get actual balance
        double balance = 0.0;

        if (balance > 0) {
            showWarning("Outstanding Balance",
                    "Cannot checkout with outstanding balance. Please settle payment first.");
            return;
        }

        // Confirm checkout
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Checkout");
        confirm.setContentText("Complete checkout for this reservation?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                reservationService.checkOut(currentReservation.getId());

                // TODO: Release rooms and notify waitlist
                // roomService.releaseRooms(...);

                showInfo("Checkout Complete",
                        "Checkout completed. Please remind guest to submit feedback at the kiosk.");

                logActivity("CHECKOUT", "Reservation", currentReservation.getId().toString(),
                        "Checkout completed");

                // Clear form
                checkoutReservationIdField.clear();
                billTextArea.clear();
                currentReservation = null;

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Checkout failed", e);
                showError("Checkout Error", "Failed to complete checkout: " + e.getMessage());
            }
        }
    }

// ============================================================================
// WAITLIST
// ============================================================================

    /**
     * Set up waitlist tab
     */
    private void setupWaitlistTab() {
        if (addToWaitlistButton != null) {
            addToWaitlistButton.setOnAction(event -> handleAddToWaitlist());
        }

        if (convertToReservationButton != null) {
            convertToReservationButton.setOnAction(event -> handleConvertWaitlist());
        }

        if (removeFromWaitlistButton != null) {
            removeFromWaitlistButton.setOnAction(event -> handleRemoveFromWaitlist());
        }

        // TODO: Set up waitlist table columns
        loadWaitlist();
    }

    /**
     * Load waitlist entries
     */
    private void loadWaitlist() {
        // TODO: Load from waitlist service
        LOGGER.info("Loading waitlist");
    }

    /**
     * Add guest to waitlist
     */
    private void handleAddToWaitlist() {
        LOGGER.info("Adding guest to waitlist");

        try {
            // Show waitlist dialog
            WaitlistDialog dialog = new WaitlistDialog();
            Optional<WaitlistData> result = dialog.showAndWait();

            if (result.isPresent()) {
                WaitlistData data = result.get();
                waitlistService.addToWaitlist(
                        data.getGuestName(),
                        data.getRoomType(),
                        data.getDesiredDate()
                );

                loadWaitlist();
                showInfo("Success", "Guest added to waitlist");
                logActivity("WAITLIST_ADD", "Waitlist", null, "Guest added to waitlist");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add to waitlist", e);
            showError("Waitlist Error", "Failed to add to waitlist: " + e.getMessage());
        }
    }

    /**
     * Convert waitlist entry to reservation
     */
    private void handleConvertWaitlist() {
        // TODO: Implement conversion from waitlist to reservation
        showInfo("Feature", "Convert waitlist entry to reservation");
    }

    /**
     * Remove from waitlist
     */
    private void handleRemoveFromWaitlist() {
        // TODO: Implement remove from waitlist
        showInfo("Feature", "Remove from waitlist");
    }

// ============================================================================
// LOYALTY
// ============================================================================

    /**
     * Set up loyalty tab
     */
    private void setupLoyaltyTab() {
        if (searchLoyaltyButton != null) {
            searchLoyaltyButton.setOnAction(event -> handleSearchLoyalty());
        }

        if (enrollGuestButton != null) {
            enrollGuestButton.setOnAction(event -> handleEnrollGuest());
        }
    }

    /**
     * Search loyalty account
     */
    private void handleSearchLoyalty() {
        String searchText = loyaltyGuestSearchField.getText().trim();

        if (searchText.isEmpty()) {
            showWarning("Input Required", "Please enter guest name or phone");
            return;
        }

        // TODO: Search for loyalty account
        LOGGER.info("Searching loyalty for: " + searchText);
    }

    /**
     * Enroll guest in loyalty program
     */
    private void handleEnrollGuest() {
        if (currentReservation == null) {
            showWarning("No Guest", "Please load a reservation first");
            return;
        }

        try {
            Guest guest = currentReservation.getGuest();

            // Confirm enrollment
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Enroll in Loyalty Program");
            confirm.setContentText(String.format(
                    "Enroll %s %s in the loyalty program?",
                    guest.getFirstName(), guest.getLastName()));

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String loyaltyNumber = loyaltyService.enrollGuest(guest);

                showInfo("Enrollment Complete",
                        "Loyalty number: " + loyaltyNumber + "\nStarting balance: 0 points");

                logActivity("LOYALTY_ENROLL", "Guest", guest.getId().toString(),
                        "Guest enrolled in loyalty program");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Enrollment failed", e);
            showError("Enrollment Error", "Failed to enroll guest: " + e.getMessage());
        }
    }

// ============================================================================
// FEEDBACK
// ============================================================================

    /**
     * Set up feedback tab
     */
    private void setupFeedbackTab() {
        if (filterRatingCombo != null) {
            filterRatingCombo.setItems(FXCollections.observableArrayList(0, 1, 2, 3, 4, 5));
            filterRatingCombo.setValue(0); // 0 = All
        }

        if (filterFeedbackButton != null) {
            filterFeedbackButton.setOnAction(event -> handleFilterFeedback());
        }

        if (exportFeedbackButton != null) {
            exportFeedbackButton.setOnAction(event -> handleExportFeedback());
        }

        // TODO: Set up feedback table columns
        loadFeedback();
    }

    /**
     * Load feedback entries
     */
    private void loadFeedback() {
        // TODO: Load from feedback service (only checked-out reservations)
        LOGGER.info("Loading feedback");
    }

    /**
     * Filter feedback
     */
    private void handleFilterFeedback() {
        Integer rating = filterRatingCombo.getValue();
        LocalDate date = filterDatePicker.getValue();

        LOGGER.info(String.format("Filtering feedback: rating=%d, date=%s", rating, date));
        // TODO: Apply filters
    }

    /**
     * Export feedback to CSV
     */
    private void handleExportFeedback() {
        try {
            // TODO: Export feedback to CSV
            showInfo("Export", "Feedback exported to feedback_export.csv");
            logActivity("EXPORT", "Feedback", null, "Feedback exported to CSV");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Export failed", e);
            showError("Export Error", "Failed to export feedback");
        }
    }

// ============================================================================
// REPORTS
// ============================================================================

    /**
     * Set up reports tab
     */
    private void setupReportsTab() {
        // Revenue report
        if (revenueTypeCombo != null) {
            revenueTypeCombo.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
            revenueTypeCombo.setValue("Daily");
        }

        if (generateRevenueReportButton != null) {
            generateRevenueReportButton.setOnAction(event -> handleGenerateRevenueReport());
        }

        if (exportRevenueButton != null) {
            exportRevenueButton.setOnAction(event -> handleExportRevenue());
        }

        // Occupancy report
        if (occupancyTypeCombo != null) {
            occupancyTypeCombo.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
            occupancyTypeCombo.setValue("Daily");
        }

        if (generateOccupancyReportButton != null) {
            generateOccupancyReportButton.setOnAction(event -> handleGenerateOccupancyReport());
        }

        if (exportOccupancyButton != null) {
            exportOccupancyButton.setOnAction(event -> handleExportOccupancy());
        }

        // Activity logs
        if (loadActivityLogsButton != null) {
            loadActivityLogsButton.setOnAction(event -> handleLoadActivityLogs());
        }

        if (exportActivityLogsButton != null) {
            exportActivityLogsButton.setOnAction(event -> handleExportActivityLogs());
        }
    }

    /**
     * Generate revenue report
     */
    private void handleGenerateRevenueReport() {
        LocalDate startDate = revenueStartDate.getValue();
        LocalDate endDate = revenueEndDate.getValue();
        String type = revenueTypeCombo.getValue();

        if (startDate == null || endDate == null) {
            showWarning("Input Required", "Please select date range");
            return;
        }

        LOGGER.info(String.format("Generating %s revenue report from %s to %s",
                type, startDate, endDate));

        // TODO: Generate and display revenue report
    }

    /**
     * Generate occupancy report
     */
    private void handleGenerateOccupancyReport() {
        LocalDate startDate = occupancyStartDate.getValue();
        LocalDate endDate = occupancyEndDate.getValue();
        String type = occupancyTypeCombo.getValue();

        if (startDate == null || endDate == null) {
            showWarning("Input Required", "Please select date range");
            return;
        }

        LOGGER.info(String.format("Generating %s occupancy report from %s to %s",
                type, startDate, endDate));

        // TODO: Generate and display occupancy report
    }

    /**
     * Load activity logs
     */
    private void handleLoadActivityLogs() {
        LOGGER.info("Loading activity logs");
        // TODO: Load from log file or audit table
    }

    /**
     * Export reports
     */
    private void handleExportRevenue() {
        // TODO: Export to CSV/PDF
        showInfo("Export", "Revenue report exported");
    }

    private void handleExportOccupancy() {
        // TODO: Export to CSV/PDF
        showInfo("Export", "Occupancy report exported");
    }

    private void handleExportActivityLogs() {
        // TODO: Export to CSV/TXT
        showInfo("Export", "Activity logs exported");
    }

// ============================================================================
// DISCOUNTS (Role-based)
// ============================================================================

    /**
     * Apply discount to reservation
     * Admin: max 15%, Manager: max 30%
     */
    private void applyDiscount(double discountPercent) {
        if (currentReservation == null) {
            showWarning("No Reservation", "Please select a reservation first");
            return;
        }

        // Check role-based caps
        double maxDiscount = currentAdmin.getRole() == AdminUser.Role.MANAGER ? 30.0 : 15.0;

        if (discountPercent > maxDiscount) {
            showWarning("Discount Limit",
                    String.format("Your role allows maximum %.0f%% discount", maxDiscount));
            return;
        }

        try {
            // TODO: Apply discount through billing service
            showInfo("Success", String.format("%.0f%% discount applied", discountPercent));
            logActivity("DISCOUNT", "Reservation", currentReservation.getId().toString(),
                    String.format("%.0f%% discount applied by %s", discountPercent, currentAdmin.getUsername()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to apply discount", e);
            showError("Discount Error", "Failed to apply discount");
        }
    }

// ============================================================================
// UTILITY METHODS
// ============================================================================

    /**
     * Log administrative activity
     */
    private void logActivity(String action, String entityType, String entityId, String message) {
        String actor = currentAdmin != null ? currentAdmin.getUsername() : "System";
        LOGGER.info(String.format("[ACTIVITY] %s - %s - %s - %s - %s",
                actor, action, entityType, entityId != null ? entityId : "N/A", message));

        // TODO: Also write to audit table if needed
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
    }

    /**
     * Show warning dialog
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============================================================================
    // PLACEHOLDER DATA TYPES AND SERVICES
    // These lightweight implementations allow the controller skeleton to compile
    // until the full UI and service layers are built out.
    // ============================================================================

    /** Minimal payment service placeholder. */
    public static class PaymentService {
        public void processPayment(Long reservationId, double amount, String method) {
            LOGGER.info(String.format("[PaymentService] Processed $%.2f via %s for reservation %d", amount, method, reservationId));
        }

        public void processRefund(Long reservationId, double amount) {
            LOGGER.info(String.format("[PaymentService] Refunded $%.2f for reservation %d", amount, reservationId));
        }
    }

    /** Minimal waitlist service placeholder. */
    public static class WaitlistService {
        public void addToWaitlist(String guestName, String roomType, LocalDate desiredDate) {
            LOGGER.info(String.format("[WaitlistService] Added %s for %s on %s", guestName, roomType, desiredDate));
        }
    }

    /** Minimal feedback service placeholder. */
    public static class FeedbackService {
    }

    // ----- DTOs used by the UI tables -----
    public static class PaymentRecord { }
    public static class WaitlistEntry { }
    public static class LoyaltyTransaction { }
    public static class FeedbackEntry { }
    public static class RevenueReport { }
    public static class OccupancyReport { }
    public static class ActivityLog { }

    // ----- Dialog placeholders used by the controller -----
    public static class ReservationDialog {
        public ReservationDialog(Reservation reservation, RoomService roomService, BillingContext billingContext) { }

        public Optional<ReservationData> showAndWait() {
            return Optional.empty();
        }
    }

    public static class ReservationDetailsDialog {
        public ReservationDetailsDialog(Reservation reservation) { }

        public void showAndWait() {
            // No-op placeholder
        }
    }

    public static class WaitlistDialog {
        public Optional<WaitlistData> showAndWait() {
            return Optional.empty();
        }
    }

    // ----- Simple data holders for dialog results -----
    public static class ReservationData {
        public String getFirstName() { return ""; }
        public String getLastName() { return ""; }
        public String getPhone() { return ""; }
        public String getEmail() { return ""; }
        public LocalDate getCheckIn() { return LocalDate.now(); }
        public LocalDate getCheckOut() { return LocalDate.now(); }
        public List<RoomType> getRooms() { return List.of(); }
        public List<String> getAddOns() { return List.of(); }
    }

    public static class WaitlistData {
        public String getGuestName() { return ""; }
        public String getRoomType() { return ""; }
        public LocalDate getDesiredDate() { return LocalDate.now(); }
    }
}
