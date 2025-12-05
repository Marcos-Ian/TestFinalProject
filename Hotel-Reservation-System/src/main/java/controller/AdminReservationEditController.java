package controller;

import app.Bootstrap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
import model.Guest;
import model.Reservation;
import model.ReservationStatus;
import model.RoomType;
import service.BillingContext;
import service.FeedbackService;
import service.LoyaltyService;
import service.ReservationConflictException;
import service.ReservationService;
import service.WaitlistService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Create/modify/cancel reservations and surface billing/loyalty context.
 */
public class AdminReservationEditController {
    private final ReservationService reservationService;
    private final BillingContext billingContext;
    private final LoyaltyService loyaltyService;
    private final WaitlistService waitlistService;
    private final FeedbackService feedbackService;
    private Reservation reservation;

    @FXML
    private TextField reservationIdField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private DatePicker checkInPicker;
    @FXML
    private DatePicker checkOutPicker;
    @FXML
    private ComboBox<ReservationStatus> statusCombo;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label baseSubtotalLabel;
    @FXML
    private Label totalAfterDiscountLabel;
    @FXML
    private TextField discountPercentField;
    @FXML
    private Button applyDiscountButton;
    @FXML
    private ListView<RoomType> roomListView;

    public AdminReservationEditController() {
        this(Bootstrap.getReservationService(), Bootstrap.getBillingContext(), Bootstrap.getLoyaltyService(), new WaitlistService(), new FeedbackService());
    }

    public AdminReservationEditController(ReservationService reservationService, BillingContext billingContext, LoyaltyService loyaltyService,
                                          WaitlistService waitlistService, FeedbackService feedbackService) {
        this.reservationService = reservationService;
        this.billingContext = billingContext;
        this.loyaltyService = loyaltyService;
        this.waitlistService = waitlistService;
        this.feedbackService = feedbackService;
        this.reservation = new Reservation();
    }
    public AdminReservationEditController(ReservationService reservationService,
                                          LoyaltyService loyaltyService,
                                          WaitlistService waitlistService,
                                          FeedbackService feedbackService) {
        this(reservationService,
                Bootstrap.getBillingContext(),   // reuse the same BillingContext source
                loyaltyService,
                waitlistService,
                feedbackService);
    }

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList(ReservationStatus.values()));
        statusCombo.getSelectionModel().select(ReservationStatus.BOOKED);
        configureRoomList();
        if (reservation == null) {
            reservation = new Reservation();
        }
    }

    @FXML
    private void onSave() {
        try {
            if (reservation == null) {
                reservation = new Reservation();
            }

            if (reservation.getGuest() == null) {
                reservation.setGuest(new Guest());
            }

            reservation.getGuest().setFirstName(firstNameField.getText());
            reservation.getGuest().setLastName(lastNameField.getText());
            reservation.getGuest().setPhoneNumber(null);

            reservation.setCheckIn(checkInPicker.getValue());
            reservation.setCheckOut(checkOutPicker.getValue());
            reservation.setStatus(statusCombo.getSelectionModel().getSelectedItem());

            List<RoomType> selectedRooms = new ArrayList<>(roomListView.getSelectionModel().getSelectedItems());
            reservation.setRooms(selectedRooms);

            reservationService.saveWithConflictCheck(reservation, selectedRooms);
            int earned = loyaltyService.calculateEarnedPoints(100.0);
            messageLabel.setText("Saved. Loyalty points estimate: " + earned);
            closeWindow();
        } catch (ReservationConflictException ex) {
            showAlert("Conflict", ex.getMessage());
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelReservation() {
        try {
            if (reservation == null) {
                return;
            }
            reservationService.cancelReservation(reservation);
            waitlistService.addToWaitlist(firstNameField.getText(), "DOUBLE", LocalDate.now());
            messageLabel.setText("Cancelled and added to waitlist");
            closeWindow();
        } catch (Exception e) {
            messageLabel.setText("Cancel failed: " + e.getMessage());
        }
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation == null ? new Reservation() : reservation;
        if (this.reservation.getGuest() == null) {
            this.reservation.setGuest(new Guest());
        }

        reservationIdField.setText(this.reservation.getId() != null ? this.reservation.getId().toString() : "");
        Guest guest = this.reservation.getGuest();
        firstNameField.setText(guest.getFirstName());
        lastNameField.setText(guest.getLastName());
        checkInPicker.setValue(this.reservation.getCheckIn());
        checkOutPicker.setValue(this.reservation.getCheckOut());
        if (this.reservation.getStatus() != null) {
            statusCombo.getSelectionModel().select(this.reservation.getStatus());
        }
        preselectRooms();
        refreshPriceView();
    }

    private void refreshPriceView() {
        if (reservation == null) return;

        double base = reservation.calculateBaseSubtotal();
        double total = billingContext.calculateTotal(reservation);

        baseSubtotalLabel.setText(String.format("$%.2f", base));
        totalAfterDiscountLabel.setText(String.format("$%.2f", total));

        discountPercentField.setText(String.format("%.2f", reservation.getDiscountPercent()));
    }

    private void configureRoomList() {
        ObservableList<RoomType> roomTypes = FXCollections.observableArrayList();
        for (RoomType.Type type : RoomType.Type.values()) {
            RoomType roomType = new RoomType();
            roomType.setType(type);
            roomTypes.add(roomType);
        }
        roomListView.setItems(roomTypes);
        roomListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        roomListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getType().name());
            }
        });
    }

    private void preselectRooms() {
        if (reservation != null && reservation.getRooms() != null) {
            for (RoomType room : reservation.getRooms()) {
                for (RoomType existing : roomListView.getItems()) {
                    if (existing.getType() == room.getType()) {
                        roomListView.getSelectionModel().select(existing);
                    }
                }
            }
        }
    }

    private void closeWindow() {
        if (saveButton != null && saveButton.getScene() != null) {
            saveButton.getScene().getWindow().hide();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onApplyDiscountClicked() {
        if (reservation == null) return;

        try {
            String text = discountPercentField.getText();
            double percent = text == null || text.isBlank() ? 0.0 : Double.parseDouble(text.trim());

            reservationService.applyDiscount(reservation.getId(), percent);

            Reservation updated = reservationService.findById(reservation.getId()).orElse(reservation);
            this.reservation = updated;

            refreshPriceView();

            showInfo("Discount applied", "Discount of " + percent + "% has been applied successfully.");
        } catch (NumberFormatException ex) {
            showError("Invalid value", "Please enter a valid discount percent.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError("Cannot apply discount", ex.getMessage());
        } catch (Exception ex) {
            showError("Error", "Unexpected error applying discount: " + ex.getMessage());
        }
    }

    private void showInfo(String title, String message) {
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
}
