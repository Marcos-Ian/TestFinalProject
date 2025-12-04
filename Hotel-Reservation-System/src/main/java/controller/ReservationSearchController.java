package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Guest;
import model.Reservation;
import security.AuthenticationService;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import service.WaitlistService;
import util.ActivityLogger;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides search and waitlist management for admins.
 */
public class ReservationSearchController {
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
    private TableColumn<Reservation, Long> idColumn;
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
            statusCombo.setItems(FXCollections.observableArrayList("All", "BOOKED", "CANCELLED", "CHECKED_OUT"));
            statusCombo.getSelectionModel().selectFirst();
        }
        configureTable();
        List<Reservation> all = reservationService.searchReservations(null, "", "", null, null, null);
        reservationTable.setItems(FXCollections.observableArrayList(all));
    }

    @FXML
    private void onSearchClicked() {
        String guest = guestField.getText();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        String status = statusCombo.getValue();
        if (status != null && status.equalsIgnoreCase("All")) {
            status = null;
        }

        List<Reservation> results =
                reservationService.searchReservations(
                        (guest == null || guest.isBlank()) ? null : guest.trim(),
                        phone,
                        email,
                        start,
                        end,
                        status
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
    private void openEdit() {
        if (editLoader != null) {
            editLoader.accept(null);
        }
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

    private void configureTable() {
        if (idColumn != null) {
            idColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        }

        if (guestColumn != null) {
            guestColumn.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getGuest() != null ?
                            c.getValue().getGuest().getFirstName() + " " + c.getValue().getGuest().getLastName() : ""));
        }

        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(cellData -> {
                Guest g = cellData.getValue().getGuest();
                String value = (g == null || g.getPhoneNumber() == null) ? "" : g.getPhoneNumber();
                return new SimpleStringProperty(value);
            });
        }

        if (checkInColumn != null) {
            checkInColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCheckIn()));
        }

        if (checkOutColumn != null) {
            checkOutColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCheckOut()));
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        }

        if (reservationTable != null && idColumn != null && guestColumn != null && phoneColumn != null
                && checkInColumn != null && checkOutColumn != null && statusColumn != null) {
            reservationTable.getColumns().setAll(idColumn, guestColumn, phoneColumn, checkInColumn, checkOutColumn, statusColumn);
        }
    }
}
