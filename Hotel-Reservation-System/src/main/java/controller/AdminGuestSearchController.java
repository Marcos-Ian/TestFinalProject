package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Guest;
import model.Reservation;
import security.AuthenticationService;
import service.GuestService;
import service.ReservationService;
import util.ActivityLogger;

import java.util.List;

public class AdminGuestSearchController {
    private final GuestService guestService;
    private final ReservationService reservationService;
    private final AuthenticationService authenticationService;

    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField addressField;
    @FXML
    private TableView<Guest> guestTable;
    @FXML
    private TableColumn<Guest, String> nameColumn;
    @FXML
    private TableColumn<Guest, String> phoneColumn;
    @FXML
    private TableColumn<Guest, String> emailColumn;
    @FXML
    private TableColumn<Guest, String> addressColumn;
    @FXML
    private TableColumn<Guest, String> loyaltyColumn;
    @FXML
    private Label resultsLabel;
    @FXML
    private Button searchButton;
    @FXML
    private Button viewDetailsButton;
    @FXML
    private Button enrollLoyaltyButton;

    public AdminGuestSearchController() {
        this(Bootstrap.getGuestService(), Bootstrap.getReservationService(), Bootstrap.getAuthenticationService());
    }

    public AdminGuestSearchController(GuestService guestService,
                                      ReservationService reservationService,
                                      AuthenticationService authenticationService) {
        this.guestService = guestService;
        this.reservationService = reservationService;
        this.authenticationService = authenticationService;
    }

    @FXML
    public void initialize() {
        if (nameColumn != null) {
            nameColumn.setCellValueFactory(c -> new SimpleStringProperty(
                    (c.getValue().getFirstName() == null ? "" : c.getValue().getFirstName()) +
                            " " +
                            (c.getValue().getLastName() == null ? "" : c.getValue().getLastName())
            ));
        }
        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhoneNumber()));
        }
        if (emailColumn != null) {
            emailColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        }
        if (addressColumn != null) {
            addressColumn.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getAddress() != null ? c.getValue().getAddress() : ""));
        }
        if (loyaltyColumn != null) {
            loyaltyColumn.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getLoyaltyNumber() != null ? c.getValue().getLoyaltyNumber() : ""));
        }
        handleSearch();
    }

    @FXML
    private void onSearchClicked() {
        handleSearch();
    }

    @FXML
    private void onViewDetailsClicked() {
        Guest guest = guestTable.getSelectionModel().getSelectedItem();
        if (guest == null) {
            showAlert("No guest selected", "Please select a guest to view details.");
            return;
        }

        List<Reservation> reservations = reservationService.findReservationsForGuest(guest);
        showGuestDetails(guest, reservations);
    }

    @FXML
    private void onEnrollLoyaltyClicked() {
        Guest guest = guestTable.getSelectionModel().getSelectedItem();
        if (guest == null) {
            showAlert("No guest selected", "Select a guest to enroll in loyalty.");
            return;
        }
        guestService.enrollInLoyalty(guest);
        handleSearch();
    }

    private void handleSearch() {
        String name = nameField != null ? nameField.getText() : "";
        String phone = phoneField != null ? phoneField.getText() : "";
        String email = emailField != null ? emailField.getText() : "";
        String address = addressField != null ? addressField.getText() : "";

        if (address != null) {
            address = address.trim();
        }

        List<Guest> results = guestService.searchGuests(name, phone, email, address);
        ObservableList<Guest> list = FXCollections.observableArrayList(results);
        guestTable.setItems(list);

        if (resultsLabel != null) {
            resultsLabel.setText(results.size() + " matching guests");
        }

        String actor = authenticationService != null && authenticationService.getCurrentUser() != null
                ? authenticationService.getCurrentUser().getUsername()
                : "UNKNOWN";
        ActivityLogger.log(actor,
                "GUEST_SEARCH",
                "Guest",
                "-",
                String.format("filters: name='%s', phone='%s', email='%s', address='%s', results=%d",
                        name, phone, email, address, results.size()));
    }

    private void showGuestDetails(Guest guest, List<Reservation> reservations) {
        VBox layout = new VBox(8);
        layout.getChildren().addAll(
                new Label("Name: " + guest.getFirstName() + " " + guest.getLastName()),
                new Label("Phone: " + guest.getPhoneNumber()),
                new Label("Email: " + guest.getEmail()),
                new Label("Address: " + valueOrEmpty(guest.getAddress())),
                new Label("Loyalty: " + (guest.getLoyaltyNumber() == null ? "Not enrolled" : guest.getLoyaltyNumber())),
                new Label("Reservations:")
        );

        TableView<Reservation> reservationTable = new TableView<>();
        TableColumn<Reservation, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(
                c.getValue().getId() == null ? 0 : c.getValue().getId()));
        TableColumn<Reservation, String> checkInCol = new TableColumn<>("Check-In");
        checkInCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCheckIn())));
        TableColumn<Reservation, String> checkOutCol = new TableColumn<>("Check-Out");
        checkOutCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCheckOut())));
        TableColumn<Reservation, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));

        reservationTable.getColumns().addAll(idCol, checkInCol, checkOutCol, statusCol);
        reservationTable.setItems(FXCollections.observableArrayList(reservations));
        reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        layout.getChildren().add(reservationTable);

        Stage stage = new Stage();
        stage.setTitle("Guest Details");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(layout, 500, 400));
        stage.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
