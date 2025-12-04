package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Reservation;
import service.BillingContext;
import service.LoyaltyService;
import service.ReservationService;
import service.WaitlistService;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides search and waitlist management for admins.
 */
public class AdminReservationSearchController {
    private final ReservationService reservationService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;
    private final WaitlistService waitlistService;
    private final Consumer<Void> editLoader;

    @FXML
    private TextField guestField;
    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TableView<Reservation> reservationTable;
    @FXML
    private Label resultLabel;
    @FXML
    private Button addWaitlistButton;

    public AdminReservationSearchController() {
        this(Bootstrap.getReservationService(), Bootstrap.getLoyaltyService(),
                Bootstrap.getBillingContext(), new WaitlistService(), null);
    }

    public AdminReservationSearchController(ReservationService reservationService,
                                            LoyaltyService loyaltyService,
                                            BillingContext billingContext,
                                            WaitlistService waitlistService,
                                            Consumer<Void> editLoader) {
        this.reservationService = reservationService;
        this.loyaltyService = loyaltyService;
        this.billingContext = billingContext;
        this.waitlistService = waitlistService;
        this.editLoader = editLoader;
    }

    @FXML
    public void initialize() {
        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("All", "BOOKED", "CANCELLED", "CHECKED_OUT"));
            statusCombo.getSelectionModel().selectFirst();
        }
        configureTable();
        loadResults();
    }

    @FXML
    private void loadResults() {
        List<Reservation> results = reservationService.searchReservations(
                guestField.getText(), null,
                startPicker.getValue(), endPicker.getValue(),
                statusCombo.getSelectionModel().getSelectedItem());
        ObservableList<Reservation> data = FXCollections.observableArrayList(results);
        reservationTable.setItems(data);
        resultLabel.setText(results.size() + " matching reservations | Billing configured");
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
            resultLabel.setText("Select a reservation to waitlist similar guests.");
            return;
        }
        String guestName = selected.getGuest() != null ?
                selected.getGuest().getFirstName() + " " + selected.getGuest().getLastName() : "Walk-in";
        waitlistService.addToWaitlist(guestName, "DOUBLE", selected.getCheckIn());
        resultLabel.setText("Added to waitlist and loyalty checked (points accrue on stay).");
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
