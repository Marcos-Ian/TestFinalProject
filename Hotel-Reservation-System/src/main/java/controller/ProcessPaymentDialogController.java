package controller;

import app.Bootstrap;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Guest;
import model.PaymentMethod;
import model.PaymentType;
import model.Reservation;
import service.PaymentService;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessPaymentDialogController {
    private static final Logger LOGGER = Logger.getLogger(ProcessPaymentDialogController.class.getName());

    @FXML
    private ComboBox<PaymentMethod> paymentMethodCombo;
    @FXML
    private ComboBox<PaymentType> paymentTypeCombo;
    @FXML
    private TextField amountField;
    @FXML
    private TextField discountField;
    @FXML
    private TextField loyaltyPointsField;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label loyaltyBalanceLabel;
    @FXML
    private Button confirmButton;

    private PaymentService paymentService;
    private Reservation reservation;
    private Runnable onPaymentProcessed;

    public ProcessPaymentDialogController() {
        try {
            this.paymentService = Bootstrap.getPaymentService();
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Payment service not available during initialization", ex);
        }
    }

    @FXML
    public void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentTypeCombo.setItems(FXCollections.observableArrayList(PaymentType.values()));
        paymentMethodCombo.getSelectionModel().selectFirst();
        paymentTypeCombo.getSelectionModel().select(PaymentType.NORMAL);
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        if (reservation == null || paymentService == null) {
            return;
        }

        BigDecimal totalPaid = paymentService.calculateTotalPaid(reservation);
        BigDecimal balance = paymentService.calculateBalance(reservation);

        totalPaidLabel.setText(formatCurrency(totalPaid.doubleValue()));
        balanceLabel.setText(formatCurrency(balance.doubleValue()));
        discountField.setText(String.format("%.2f", reservation.getDiscountPercent()));

        Guest guest = reservation.getGuest();
        int availablePoints = guest != null ? guest.getLoyaltyPoints() : 0;
        loyaltyBalanceLabel.setText(String.valueOf(availablePoints));
    }

    public void setOnPaymentProcessed(Runnable onPaymentProcessed) {
        this.onPaymentProcessed = onPaymentProcessed;
    }

    @FXML
    private void handleConfirm() {
        try {
            PaymentMethod method = paymentMethodCombo.getValue();
            PaymentType type = paymentTypeCombo.getValue();
            double amount = parseDouble(amountField.getText(), "Amount");
            double percentDiscount = parseDouble(discountField.getText(), "Discount", true);
            int pointsToRedeem = parseInt(loyaltyPointsField.getText());

            if (paymentService == null || reservation == null) {
                throw new IllegalStateException("Payment service is not available");
            }

            paymentService.processPayment(reservation.getId(), amount, method, type, percentDiscount, pointsToRedeem);

            if (onPaymentProcessed != null) {
                onPaymentProcessed.run();
            }
            closeDialog();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Payment Error", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unexpected payment error", ex);
            showAlert(Alert.AlertType.ERROR, "Payment Error", "Unable to process payment.");
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private double parseDouble(String raw, String fieldName) {
        return parseDouble(raw, fieldName, false);
    }

    private double parseDouble(String raw, String fieldName, boolean allowEmpty) {
        if ((raw == null || raw.isBlank()) && allowEmpty) {
            return 0.0;
        }
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + fieldName.toLowerCase());
        }
    }

    private int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new IllegalArgumentException("Loyalty points cannot be negative.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid loyalty points value.");
        }
    }

    private String formatCurrency(double value) {
        return String.format("$%,.2f", value);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeDialog() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }
}
