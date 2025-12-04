package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Guest;
import model.Reservation;
import service.LoyaltyService;
import service.ReservationService;
import service.WaitlistService;
import service.FeedbackService;

import java.time.LocalDate;

/**
 * Create/modify/cancel reservations and surface billing/loyalty context.
 */
public class AdminReservationEditController {
    private final ReservationService reservationService;
    private final LoyaltyService loyaltyService;
    private final WaitlistService waitlistService;
    private final FeedbackService feedbackService;

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
    private ComboBox<String> statusCombo;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    public AdminReservationEditController() {
        this(Bootstrap.getReservationService(), Bootstrap.getLoyaltyService(), new WaitlistService(), new FeedbackService());
    }

    public AdminReservationEditController(ReservationService reservationService, LoyaltyService loyaltyService,
                                          WaitlistService waitlistService, FeedbackService feedbackService) {
        this.reservationService = reservationService;
        this.loyaltyService = loyaltyService;
        this.waitlistService = waitlistService;
        this.feedbackService = feedbackService;
    }

    @FXML
    public void initialize() {
        statusCombo.setItems(javafx.collections.FXCollections.observableArrayList("BOOKED", "CANCELLED", "CHECKED_OUT"));
        statusCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void saveReservation() {
        try {
            Reservation reservation;
            if (!reservationIdField.getText().isBlank()) {
                Long id = Long.parseLong(reservationIdField.getText());
                reservation = reservationService.findById(id).orElse(new Reservation());
            } else {
                reservation = new Reservation();
            }

            Guest guest = reservation.getGuest() != null ? reservation.getGuest() : new Guest();
            guest.setFirstName(firstNameField.getText());
            guest.setLastName(lastNameField.getText());
            reservation.setGuest(guest);
            reservation.setCheckIn(checkInPicker.getValue());
            reservation.setCheckOut(checkOutPicker.getValue());
            reservation.setStatus(statusCombo.getSelectionModel().getSelectedItem());

            if (reservation.getId() == null) {
                reservation = reservationService.createReservation(guest, reservation.getCheckIn(), reservation.getCheckOut());
                reservation.setStatus(statusCombo.getSelectionModel().getSelectedItem());
                reservationService.updateReservation(reservation);
            } else {
                reservationService.updateReservation(reservation);
            }

            int earned = loyaltyService.calculateEarnedPoints(100.0);
            messageLabel.setText("Saved. Loyalty points estimate: " + earned);
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void cancelReservation() {
        try {
            Long id = Long.parseLong(reservationIdField.getText());
            reservationService.cancelReservation(id);
            waitlistService.addToWaitlist(firstNameField.getText(), "DOUBLE", LocalDate.now());
            messageLabel.setText("Cancelled and added to waitlist");
        } catch (Exception e) {
            messageLabel.setText("Cancel failed: " + e.getMessage());
        }
    }
}
