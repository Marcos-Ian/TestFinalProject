package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class ReservationController {

    // ─────────── TOP BAR ───────────
    @FXML private Button backButton;
    @FXML private Label  titleLabel;
    @FXML private Label  reservationIdLabel;
    @FXML private Label  statusBadge;

    // ─────────── GUEST DETAILS ───────────
    @FXML private Label guestNameLabel;
    @FXML private Label guestPhoneLabel;
    @FXML private Label guestEmailLabel;

    @FXML private Label loyaltyStatusLabel;
    @FXML private Button enrollLoyaltyButton;

    @FXML private VBox   loyaltyNumberSection;
    @FXML private Label  loyaltyNumberLabel;

    @FXML private VBox   loyaltyPointsSection;
    @FXML private Label  loyaltyPointsLabel;

    // ─────────── RESERVATION DETAILS ───────────
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label nightsLabel;
    @FXML private Label guestsLabel;
    @FXML private Label bookingDateLabel;
    @FXML private Label bookedByLabel;

    // ─────────── ROOM TABLE ───────────
    @FXML private TableView<?>      roomsTable;
    @FXML private TableColumn<?, ?> roomTypeCol;
    @FXML private TableColumn<?, ?> roomQuantityCol;
    @FXML private TableColumn<?, ?> roomCapacityCol;
    @FXML private TableColumn<?, ?> roomPriceCol;
    @FXML private TableColumn<?, ?> roomTotalCol;

    // ─────────── ADD-ON SERVICES TABLE ───────────
    @FXML private TableView<?>      addonsTable;
    @FXML private TableColumn<?, ?> addonNameCol;
    @FXML private TableColumn<?, ?> addonPricingCol;
    @FXML private TableColumn<?, ?> addonPriceCol;
    @FXML private TableColumn<?, ?> addonTotalCol;

    // ─────────── FINANCIAL SUMMARY ───────────
    @FXML private Label roomSubtotalLabel;
    @FXML private Label addonsTotalLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;

    @FXML private Label discountRow;
    @FXML private Label discountLabel;

    @FXML private Label loyaltyRedemptionRow;
    @FXML private Label loyaltyRedemptionLabel;

    @FXML private Label grandTotalLabel;
    @FXML private Label paidLabel;
    @FXML private Label balanceLabel;

    // ─────────── PAYMENTS TABLE ───────────
    @FXML private TableView<?>      paymentsTable;
    @FXML private TableColumn<?, ?> paymentDateCol;
    @FXML private TableColumn<?, ?> paymentTypeCol;
    @FXML private TableColumn<?, ?> paymentAmountCol;
    @FXML private TableColumn<?, ?> paymentByCol;
    @FXML private TableColumn<?, ?> paymentNotesCol;

    // ─────────── ACTION BUTTONS ───────────
    @FXML private Button modifyButton;
    @FXML private Button applyDiscountButton;
    @FXML private Button processPaymentButton;
    @FXML private Button checkoutButton;
    @FXML private Button cancelButton;

    // ─────────── LIFECYCLE ───────────
    @FXML
    public void initialize() {
        // Runs after FXML loads; put any setup logic here.
        // For now, we can just ensure sections respect their visibility flags.
        syncSectionVisibility();
    }

    private void syncSectionVisibility() {
        // make sure managed matches visible so hidden sections don’t take space
        if (loyaltyNumberSection != null) {
            loyaltyNumberSection.setManaged(loyaltyNumberSection.isVisible());
        }
        if (loyaltyPointsSection != null) {
            loyaltyPointsSection.setManaged(loyaltyPointsSection.isVisible());
        }
    }

    // Later you can add @FXML handlers, e.g.:
    //
    // @FXML
    // private void onBack() { /* navigate back */ }
    //
    // @FXML
    // private void onApplyDiscount() { /* open discount dialog */ }
}
