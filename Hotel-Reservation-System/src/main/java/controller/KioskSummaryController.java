package controller;

import app.Bootstrap;
import config.LoyaltyConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.Reservation;
import service.ReservationService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Displays booking summary and triggers reservation persistence.
 */
public class KioskSummaryController {
    private final ReservationService reservationService;
    private final LoyaltyConfig loyaltyConfig;
    private final KioskFlowContext context;

    @FXML
    private Label guestLabel;
    @FXML
    private Label datesLabel;
    @FXML
    private Label roomLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label loyaltyLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<String> addOnList;
    @FXML
    private Button confirmButton;

    public KioskSummaryController() {
        this(Bootstrap.getReservationService(), Bootstrap.getLoyaltyConfig(), KioskFlowContext.getInstance());
    }

    public KioskSummaryController(ReservationService reservationService, LoyaltyConfig loyaltyConfig, KioskFlowContext context) {
        this.reservationService = reservationService;
        this.loyaltyConfig = loyaltyConfig;
        this.context = context;
    }

    @FXML
    public void initialize() {
        guestLabel.setText(context.getGuest().getFirstName() + " " + context.getGuest().getLastName());
        LocalDate in = context.getCheckIn();
        LocalDate out = context.getCheckOut();
        datesLabel.setText(String.format("%s to %s", in, out));

        if (!context.getSelectedRooms().isEmpty()) {
            roomLabel.setText(context.getSelectedRooms().get(0).getType().name());
        }
        addOnList.getItems().setAll(context.getAddOns());
        totalLabel.setText(String.format("Estimated total: $%.2f", context.getEstimatedTotal()));
        int earned = loyaltyConfig.calculatePointsEarned(context.getEstimatedTotal());
        loyaltyLabel.setText("Points to earn: " + earned);
    }

    @FXML
    private void confirmReservation() throws IOException {
        Reservation reservation = new Reservation();
        reservation.setGuest(context.getGuest());
        reservation.setCheckIn(context.getCheckIn());
        reservation.setCheckOut(context.getCheckOut());
        reservation.setStatus("BOOKED");

        reservationService.createReservation(reservation, context.getSelectedRooms(), context.getAddOns());
        statusLabel.setText("Reservation saved. An associate will finalize billing.");
        loadScene("/view/kiosk_welcome.fxml");
    }

    private void loadScene(String resource) throws IOException {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Parent root = loader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }
}
