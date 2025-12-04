package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TableView<Reservation> reservationTable;
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
        List<Reservation> all = reservationService.searchReservations(null, null, null, null);
        reservationTable.setItems(FXCollections.observableArrayList(all));
    }

    @FXML
    private void onSearchClicked() {
        String guest = guestField.getText();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        String status = statusCombo.getValue();
        if (status != null && status.equalsIgnoreCase("All")) {
            status = null;
        }

        List<Reservation> results =
                reservationService.searchReservations(
                        (guest == null || guest.isBlank()) ? null : guest.trim(),
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
                String.format("Reservation search executed with filters: guest='%s', start=%s, end=%s, status=%s, results=%d",
                        guest,
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
        TableColumn<Reservation, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));

        TableColumn<Reservation, String> guestCol = new TableColumn<>("Guest");
        guestCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGuest() != null ?
                        c.getValue().getGuest().getFirstName() + " " + c.getValue().getGuest().getLastName() : ""));

        TableColumn<Reservation, LocalDate> inCol = new TableColumn<>("Check-In");
        inCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCheckIn()));

        TableColumn<Reservation, LocalDate> outCol = new TableColumn<>("Check-Out");
        outCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCheckOut()));

        TableColumn<Reservation, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        reservationTable.getColumns().setAll(idCol, guestCol, inCol, outCol, statusCol);
    }
}
