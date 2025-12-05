package controller;

import app.Bootstrap;
import config.LoyaltyConfig;
import config.PricingConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.Guest;
import model.Reservation;
import model.RoomType;
import model.ReservationStatus;
import service.BillingContext;
import service.ReservationService;
import javafx.scene.paint.Color;   // ⬅️ add this import at the top

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Displays booking summary and handles confirmation for Step 4.
 */
public class KioskSummaryController {
    private final ReservationService reservationService;
    private final LoyaltyConfig loyaltyConfig;
    private final PricingConfig pricingConfig;
    private final BillingContext billingContext;
    private final KioskFlowContext context;

    @FXML
    private Label guestsLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Label datesLabel;
    @FXML
    private Label nightsLabel;
    @FXML
    private Label roomsLabel;
    @FXML
    private Label addOnsLabel;
    @FXML
    private Label roomSubtotalLabel;
    @FXML
    private Label addOnSubtotalLabel;
    @FXML
    private Label taxLabel;
    @FXML
    private Label loyaltyLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<String> addOnList;
    @FXML
    private Button confirmButton;
    @FXML
    private Button finishButton;

    public KioskSummaryController() {
        this(KioskFlowContext.getInstance());
    }

    public KioskSummaryController(KioskFlowContext context) {
        this(Bootstrap.getReservationService(), Bootstrap.getLoyaltyConfig(), Bootstrap.getPricingConfig(), Bootstrap.getBillingContext(), context);
    }

    public KioskSummaryController(ReservationService reservationService, LoyaltyConfig loyaltyConfig, PricingConfig pricingConfig, BillingContext billingContext, KioskFlowContext context) {
        this.reservationService = reservationService;
        this.loyaltyConfig = loyaltyConfig;
        this.pricingConfig = pricingConfig;
        this.billingContext = billingContext;
        this.context = context;
    }

    @FXML
    public void initialize() {
        guestsLabel.setText(String.format("%d adults, %d children", context.getAdults(), context.getChildren()));
        Guest guest = context.getGuest();
        addressLabel.setText(guest != null && guest.getAddress() != null ? guest.getAddress() : "");
        LocalDate in = context.getCheckIn();
        LocalDate out = context.getCheckOut();
        long nights = in != null && out != null ? ChronoUnit.DAYS.between(in, out) : 0;
        datesLabel.setText(in != null && out != null ? String.format("%s to %s", in, out) : "");
        nightsLabel.setText(nights > 0 ? nights + " night(s)" : "");

        List<RoomType> rooms = context.getSelectedRooms();
        String roomPlan = rooms.isEmpty() ? "No rooms selected" : rooms.stream()
                .collect(Collectors.groupingBy(RoomType::getType, Collectors.counting()))
                .entrySet().stream()
                .map(e -> e.getValue() + " × " + e.getKey().name())
                .collect(Collectors.joining(", "));
        roomsLabel.setText(roomPlan);

        addOnList.getItems().setAll(context.getAddOns());
        addOnsLabel.setText(context.getAddOns().isEmpty() ? "None" : String.join(", ", context.getAddOns()));

        if (rooms.isEmpty() || in == null || out == null) {
            confirmButton.setDisable(true);
            statusLabel.setText("Missing booking details. Please go back.");
            return;
        }

        KioskPricingHelper.BookingBreakdown breakdown = KioskPricingHelper.calculate(rooms, context.getAddOns(), in, out, billingContext, pricingConfig);
        context.setEstimatedTotal(breakdown.total());
        context.setRoomSubtotal(breakdown.roomSubtotal());
        context.setAddOnSubtotal(breakdown.addOnSubtotal());
        context.setTax(breakdown.tax());

        roomSubtotalLabel.setText(String.format("$%.2f", breakdown.roomSubtotal()));
        addOnSubtotalLabel.setText(String.format("$%.2f", breakdown.addOnSubtotal()));
        taxLabel.setText(String.format("$%.2f", breakdown.tax()));
        totalLabel.setText(String.format("$%.2f", breakdown.total()));

        int loyaltyPoints = loyaltyConfig.calculatePointsEarned(breakdown.total());
        loyaltyLabel.setText("Earn on completion: " + loyaltyPoints + " pts");
    }

    @FXML
    private void backToGuestDetails() throws IOException {
        loadScene("/view/kiosk_guest_details.fxml");
    }

    @FXML
    private void confirmReservation() {
        try {
            Reservation reservation = new Reservation();
            reservation.setGuest(context.getGuest());
            reservation.setCheckIn(context.getCheckIn());
            reservation.setCheckOut(context.getCheckOut());
            reservation.setStatus(ReservationStatus.BOOKED);

            // This will run validateGuest(...) and can throw IllegalArgumentException
            reservationService.createReservation(
                    reservation,
                    context.getSelectedRooms(),
                    context.getAddOns()
            );

            statusLabel.setText("Your reservation has been saved. Billing will be handled at the front desk.");
            statusLabel.setTextFill(Color.GREEN);
            confirmButton.setDisable(true);
        } catch (IllegalArgumentException ex) {
            // e.g. "Valid 10-digit phone number is required"
            statusLabel.setText(ex.getMessage());
            statusLabel.setTextFill(Color.RED);
            ex.printStackTrace();
        } catch (Exception ex) {
            // Any unexpected errors – don't crash the app
            statusLabel.setText("Unexpected error while confirming reservation.");
            statusLabel.setTextFill(Color.RED);
            ex.printStackTrace();
        }
    }

    @FXML
    private void onFinishClicked() {
        try {
            context.reset();

            Stage stage = (Stage) finishButton.getScene().getWindow();
            Parent root = FXMLLoader.load(
                    getClass().getResource("/view/main.fxml")
            );

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
