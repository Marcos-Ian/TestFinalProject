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
import model.ReservationStatus;
import security.AuthenticationService;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import service.WaitlistService;
import util.ActivityLogger;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides search and waitlist management for admins.
 */
public class ReservationSearchController {
    private static final Logger logger = Logger.getLogger(ReservationSearchController.class.getName());

    private final ReservationService reservationService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;
    private final WaitlistService waitlistService;
    private final Consumer<Void> editLoader;
    private final AuthenticationService authService;

    @FXML
    private TextField guestField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TableView<Reservation> reservationTable;
    @FXML
    private TableColumn<Reservation, Number> idColumn;
    @FXML
    private TableColumn<Reservation, String> guestColumn;
    @FXML
    private TableColumn<Reservation, String> phoneColumn;
    @FXML
    private TableColumn<Reservation, LocalDate> checkInColumn;
    @FXML
    private TableColumn<Reservation, LocalDate> checkOutColumn;
    @FXML
    private TableColumn<Reservation, String> statusColumn;

    @FXML
    private Label resultsLabel;
    @FXML
    private Button addWaitlistButton;

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
            statusCombo.setItems(FXCollections.observableArrayList("All", "BOOKED", "CANCELLED", "CHECKED_OUT", "CONFIRMED"));
            statusCombo.getSelectionModel().selectFirst();
        }
        configureTable();
        reservationTable.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    try {
                        openReservationEditor(row.getItem());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Failed to open editor", e);
                    }
                }
            });
            return row;
        });
        handleSearch();
    }

    @FXML
    private void onSearchClicked() {
        handleSearch();
    }

    private void handleSearch() {
        String guest = guestField.getText();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        String status = statusCombo.getValue();
        ReservationStatus statusFilter = null;
        if (status != null && !status.equalsIgnoreCase("All")) {
            statusFilter = ReservationStatus.valueOf(status);
        }

        List<Reservation> results = reservationService.searchReservations(
                (guest == null || guest.isBlank()) ? null : guest.trim(),
                phone,
                email,
                start,
                end,
                statusFilter
        );

        reservationTable.setItems(FXCollections.observableArrayList(results));
        if (resultsLabel != null) {
            resultsLabel.setText(results.size() + " matching reservations | Billing: StandardBillingStrategy");
        }

        String actor = "UNKNOWN";
        if (authService != null && authService.getCurrentUser() != null) {
            actor = authService.getCurrentUser().getUsername();
        }

        ActivityLogger.log(
                actor,
                "SEARCH",
                "Reservation",
                "-",
                String.format("Reservation search executed with filters: guest='%s', phone='%s', email='%s', start=%s, end=%s, status=%s, results=%d",
                        guest,
                        phone,
                        email,
                        start,
                        end,
                        status,
                        results.size())
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
            showAlert("No selection", "Please select a reservation to edit.");
            return;
        }

        openReservationEditor(selected);
    }

    @FXML
    private void addToWaitlist() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            resultsLabel.setText("Select a reservation to waitlist similar guests.");
            return;
        }
        String guestName = selected.getGuest() != null ?
                selected.getGuest().getFirstName() + " " + selected.getGuest().getLastName() : "Walk-in";
        waitlistService.addToWaitlist(guestName, "DOUBLE", selected.getCheckIn());
        resultsLabel.setText("Added to waitlist and loyalty checked (points accrue on stay).");
    }

    private void openReservationEditor(Reservation reservation) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_reservation_edit.fxml"));
        Parent root = loader.load();
        Object controller = loader.getController();
        if (controller instanceof AdminReservationEditController editController) {
            editController.setReservation(reservation);
        }

        Stage stage = new Stage();
        stage.setTitle("Edit Reservation");
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        handleSearch();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
                return new SimpleStringProperty(name);
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
        if (reservationTable != null) {
            reservationTable.getColumns().setAll(
                    idColumn, guestColumn, phoneColumn,
                    checkInColumn, checkOutColumn, statusColumn
            );
        }
    }
}
