package controller;

import app.Bootstrap;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableRow;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides search and waitlist management for admins.
 */
public class ReservationSearchController {
    private static final Logger logger = Logger.getLogger(ReservationSearchController.class.getName());
    private static final int ROWS_PER_PAGE = 10;

    private final ReservationService reservationService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;
    private final WaitlistService waitlistService;
    private final Consumer<Void> editLoader;
    private final AuthenticationService authService;
    private List<Reservation> allResults = new ArrayList<>();

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
    private TableColumn<Reservation, ReservationStatus> statusColumn;

    @FXML
    private Label resultsLabel;
    @FXML
    private Button addWaitlistButton;
    @FXML
    private Pagination reservationPagination;

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
        reservationTable.getSortOrder().addListener((ListChangeListener<TableColumn<Reservation, ?>>) c -> applySortingAndRefreshPage());
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

        allResults = results;

        int pageCount = (int) Math.ceil((double) allResults.size() / ROWS_PER_PAGE);
        if (pageCount == 0) pageCount = 1;

        reservationPagination.setPageCount(pageCount);
        reservationPagination.setCurrentPageIndex(0);
        reservationPagination.setPageFactory(this::createPage);
        if (resultsLabel != null) {
            resultsLabel.setText(allResults.size() + " matching reservations | Billing: StandardBillingStrategy");
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
                        allResults.size())
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

    private Node createPage(int pageIndex) {
        if (allResults == null || allResults.isEmpty()) {
            reservationTable.setItems(FXCollections.observableArrayList());
            return reservationTable;
        }

        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allResults.size());

        if (fromIndex > toIndex) {
            reservationTable.setItems(FXCollections.observableArrayList());
            return reservationTable;
        }

        ObservableList<Reservation> pageData =
                FXCollections.observableArrayList(allResults.subList(fromIndex, toIndex));

        reservationTable.setItems(pageData);
        return reservationTable;
    }

    private void applySortingAndRefreshPage() {
        if (allResults == null || reservationTable == null || reservationPagination == null) {
            return;
        }

        // Use the TableView's comparator (built from the sorted column + sort type)
        Comparator<? super Reservation> comparator = reservationTable.getComparator();
        if (comparator != null) {
            allResults.sort(comparator);   // ✅ sort the backing List
        }

        // Rebuild current page after sorting
        int currentPage = reservationPagination.getCurrentPageIndex();
        reservationPagination.setPageFactory(this::createPage);
        reservationPagination.setCurrentPageIndex(currentPage);
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
            idColumn.setCellValueFactory(c ->
                    new SimpleLongProperty(c.getValue().getId()).asObject());
            idColumn.setSortable(true);
        }

        if (guestColumn != null) {
            guestColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getGuest().getFullName()));
            guestColumn.setSortable(true);
        }

        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getGuest().getPhoneNumber()));
            phoneColumn.setSortable(true);
        }

        if (checkInColumn != null) {
            checkInColumn.setCellValueFactory(c ->
                    new SimpleObjectProperty<>(c.getValue().getCheckIn()));
            checkInColumn.setSortable(true);
        }

        if (checkOutColumn != null) {
            checkOutColumn.setCellValueFactory(c ->
                    new SimpleObjectProperty<>(c.getValue().getCheckOut()));
            checkOutColumn.setSortable(true);
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getStatus().name()));
            statusColumn.setSortable(true);
        }

        if (reservationTable != null && idColumn != null && guestColumn != null && phoneColumn != null
                && checkInColumn != null && checkOutColumn != null && statusColumn != null) {
            reservationTable.getColumns().setAll(idColumn, guestColumn, phoneColumn, checkInColumn, checkOutColumn, statusColumn);
        }
    }
}
