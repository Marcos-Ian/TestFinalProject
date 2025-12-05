package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Reservation;
import model.ReservationAddOn;
import model.ReservationStatus;
import model.RoomType;
import security.AuthenticationService;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import service.WaitlistService;
import util.ActivityLogger;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for searching and viewing reservations
 */
public class ReservationSearchController {
    private static final Logger logger = Logger.getLogger(ReservationSearchController.class.getName());

    private final ReservationService reservationService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;
    private final WaitlistService waitlistService;
    private final Consumer<Void> editLoader;
    private final AuthenticationService authService;

    @FXML private TextField guestField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TableView<Reservation> reservationTable;
    @FXML private TableColumn<Reservation, Number> idColumn;
    @FXML private TableColumn<Reservation, String> guestColumn;
    @FXML private TableColumn<Reservation, String> phoneColumn;
    @FXML private TableColumn<Reservation, LocalDate> checkInColumn;
    @FXML private TableColumn<Reservation, LocalDate> checkOutColumn;
    @FXML private TableColumn<Reservation, String> statusColumn;
    @FXML private Label resultsLabel;
    @FXML private Button addWaitlistButton;

    public ReservationSearchController() {
        this(Bootstrap.getReservationService(), Bootstrap.getLoyaltyService(),
                Bootstrap.getBillingContext(), new WaitlistService(), null,
                Bootstrap.getAuthenticationService());
    }

    public ReservationSearchController(ReservationService reservationService,
                                       LoyaltyService loyaltyService,
                                       BillingContext billingContext,
                                       WaitlistService waitlistService,
                                       Consumer<Void> editLoader,
                                       AuthenticationService authService) {
        this.reservationService = reservationService;
        this.loyaltyService = loyaltyService;
        this.billingContext = billingContext;
        this.waitlistService = waitlistService;
        this.editLoader = editLoader;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList(
                    "All", "BOOKED", "CHECKED_IN", "COMPLETED", "CANCELLED", "CHECKED_OUT", "CONFIRMED"));
            statusCombo.getSelectionModel().selectFirst();
        }
        configureTable();

        // Configure double-click to open details
        if (reservationTable != null) {
            reservationTable.setRowFactory(tv -> {
                TableRow<Reservation> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        try {
                            openReservationDetails(row.getItem());
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Failed to open details", e);
                            showError("Error", "Failed to open reservation details: " + e.getMessage());
                        }
                    }
                });
                return row;
            });
        }

        handleSearch();
    }

    @FXML
    private void onSearchClicked() {
        handleSearch();
    }

    private void handleSearch() {
        String guest = guestField != null ? guestField.getText() : "";
        String phone = phoneField != null ? phoneField.getText() : "";
        String email = emailField != null ? emailField.getText() : "";
        LocalDate start = startDatePicker != null ? startDatePicker.getValue() : null;
        LocalDate end = endDatePicker != null ? endDatePicker.getValue() : null;

        phone = phone == null ? "" : phone.trim();
        email = email == null ? "" : email.trim();

        String status = statusCombo != null ? statusCombo.getValue() : "All";
        ReservationStatus statusFilter = null;
        if (status != null && !status.equalsIgnoreCase("All")) {
            try {
                statusFilter = ReservationStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid status: " + status);
            }
        }

        List<Reservation> results = reservationService.searchReservations(
                (guest == null || guest.isBlank()) ? null : guest.trim(),
                phone,
                email,
                start,
                end,
                statusFilter
        );

        if (reservationTable != null) {
            reservationTable.setItems(FXCollections.observableArrayList(results));
        }

        if (resultsLabel != null) {
            resultsLabel.setText(results.size() + " matching reservations");
        }

        String actor = "UNKNOWN";
        if (authService != null && authService.getCurrentUser() != null) {
            actor = authService.getCurrentUser().getUsername();
        }

        ActivityLogger.log(
                actor,
                "RESERVATION_SEARCH",
                "Reservation",
                "-",
                String.format("Search: guest='%s', phone='%s', email='%s', start=%s, end=%s, status=%s, results=%d",
                        guest, phone, email, start, end, status, results.size())
        );
    }

    @FXML
    private void openEdit() throws IOException {
        onEditReservation();
    }

    @FXML
    private void onEditReservation() throws IOException {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Please select a reservation to view.");
            return;
        }

        openReservationDetails(selected);
    }

    @FXML
    private void addToWaitlist() {
        Reservation selected = reservationTable != null ?
                reservationTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (resultsLabel != null) {
                resultsLabel.setText("Select a reservation to waitlist similar guests.");
            }
            return;
        }
        String guestName = selected.getGuest() != null ?
                selected.getGuest().getFirstName() + " " + selected.getGuest().getLastName() : "Walk-in";
        waitlistService.addToWaitlist(guestName, "DOUBLE", selected.getCheckIn());
        if (resultsLabel != null) {
            resultsLabel.setText("Added to waitlist");
        }
    }

    /**
     * Open the detailed reservation view
     */
    // UPDATE this method in ReservationSearchController.java

    /**
     * Open the detailed reservation view
     */
    private void openReservationDetails(Reservation reservation) throws IOException {
        logger.info("Opening details for reservation: " + reservation.getId());

        // Try to load the FXML file
        java.net.URL fxmlLocation = getClass().getResource("/view/ReservationDetails.fxml");
        if (fxmlLocation == null) {
            logger.severe("FXML file not found at: /view/ReservationDetails.fxml");
            showError("File Not Found",
                    "Could not find ReservationDetails.fxml\n\n" +
                            "Please ensure the file exists at:\n" +
                            "src/main/resources/view/ReservationDetails.fxml");
            return;
        }

        logger.info("Loading FXML from: " + fxmlLocation);
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        Object controller = loader.getController();

        if (controller instanceof ReservationController detailsController) {
            // Get rooms from reservation
            List<RoomType> rooms = reservation.getRooms() != null ?
                    new ArrayList<>(reservation.getRooms()) : new ArrayList<>();

            logger.info("Reservation has " + rooms.size() + " rooms");

            // Get add-ons from reservation entity
            List<String> addOns = new ArrayList<>();
            if (reservation.getAddOns() != null) {
                for (ReservationAddOn addOn : reservation.getAddOns()) {
                    addOns.add(addOn.getAddOnName());
                }
            }
            logger.info("Reservation has " + addOns.size() + " add-ons");

            // Get payment history (for now empty, can be implemented later)
            List<ReservationController.PaymentRow> payments = new ArrayList<>();

            // Calculate total guests from rooms
            int totalGuests = rooms.stream()
                    .mapToInt(RoomType::getCapacity)
                    .sum();
            if (totalGuests == 0) totalGuests = 2; // Default

            // Get amount paid (for now 0, implement payment tracking later)
            double amountPaid = 0.0;

            // Get discount from reservation
            double discount = reservation.getDiscountPercent() != null ?
                    reservation.getDiscountPercent() : 0.0;

            // Get loyalty redemption (for now 0, implement later)
            double loyaltyRedemption = 0.0;

            // Determine who booked it
            String bookedBy = "Kiosk"; // TODO: Get from reservation metadata

            logger.info("Displaying reservation: guests=" + totalGuests +
                    ", discount=" + discount + "%, paid=$" + amountPaid +
                    ", add-ons=" + addOns.size());

            // Display the reservation with actual data
            detailsController.displayReservation(
                    reservation, rooms, addOns, payments, totalGuests,
                    amountPaid, discount, loyaltyRedemption, bookedBy
            );
        } else {
            logger.warning("Controller is not ReservationController: " +
                    (controller != null ? controller.getClass().getName() : "null"));
        }

        Stage stage = new Stage();
        stage.setTitle("Reservation Details - #" + reservation.getId());
        stage.setScene(new Scene(root, 900, 700));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        // Refresh search results after closing details
        handleSearch();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    private void configureTable() {
        if (idColumn != null) {
            idColumn.setCellValueFactory(c -> {
                Long id = c.getValue().getId();
                return new SimpleLongProperty(id == null ? 0L : id);
            });
        }
        if (guestColumn != null) {
            guestColumn.setCellValueFactory(c -> {
                var g = c.getValue().getGuest();
                if (g == null) {
                    return new SimpleStringProperty("Walk-in");
                }
                String first = g.getFirstName() == null ? "" : g.getFirstName();
                String last = g.getLastName() == null ? "" : g.getLastName();
                String name = (first + " " + last).trim();
                return new SimpleStringProperty(name.isEmpty() ? "Unknown" : name);
            });
        }
        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(c -> {
                var g = c.getValue().getGuest();
                String phone = (g == null || g.getPhoneNumber() == null)
                        ? "" : g.getPhoneNumber();
                return new SimpleStringProperty(phone);
            });
        }
        if (checkInColumn != null) {
            checkInColumn.setCellValueFactory(c ->
                    new SimpleObjectProperty<>(c.getValue().getCheckIn()));
        }
        if (checkOutColumn != null) {
            checkOutColumn.setCellValueFactory(c ->
                    new SimpleObjectProperty<>(c.getValue().getCheckOut()));
        }
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(
                            c.getValue().getStatus() == null
                                    ? "" : c.getValue().getStatus().name()));
        }
    }
}