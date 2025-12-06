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
import model.PaymentMethod;
import model.PaymentType;
import model.Reservation;
import service.LoyaltyService;
import service.PaymentService;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessPaymentController {
    private static final Logger LOGGER = Logger.getLogger(ProcessPaymentController.class.getName());

    @FXML
    private ComboBox<PaymentMethod> paymentMethodCombo;
    @FXML
    private ComboBox<PaymentType> paymentTypeCombo;
    @FXML
    private TextField amountField;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label previewRemainingLabel;
    @FXML
    private Button confirmButton;

    private final PaymentService paymentService;
    private final LoyaltyService loyaltyService;

    private Reservation reservation;
    private Long reservationId;
    private BigDecimal currentBalance = BigDecimal.ZERO;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private Runnable onPaymentProcessed;

    public ProcessPaymentController() {
        PaymentService payService;
        LoyaltyService loyService;
        try {
            payService = Bootstrap.getPaymentService();
            loyService = Bootstrap.getLoyaltyService();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to load services from Bootstrap", ex);
            payService = null;
            loyService = null;
        }
        this.paymentService = payService;
        this.loyaltyService = loyService;
    }

    @FXML
    public void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentTypeCombo.setItems(FXCollections.observableArrayList(PaymentType.values()));
        paymentTypeCombo.getSelectionModel().select(PaymentType.NORMAL);

        amountField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        paymentTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        this.reservationId = reservation != null ? reservation.getId() : null;
        refreshFinancials();
    }

    public void setOnPaymentProcessed(Runnable onPaymentProcessed) {
        this.onPaymentProcessed = onPaymentProcessed;
    }

    @FXML
    private void handleConfirm() {
        try {
            PaymentMethod method = paymentMethodCombo.getValue();
            PaymentType type = paymentTypeCombo.getValue();
            BigDecimal amount = parseAmount(amountField.getText());

            if (reservationId == null || paymentService == null) {
                throw new IllegalStateException("Payment service is not available");
            }

            paymentService.processPayment(reservationId, method, type, amount);
            closeDialog();

            if (onPaymentProcessed != null) {
                onPaymentProcessed.run();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Payment processing failed", ex);
            showAlert(Alert.AlertType.ERROR, "Payment Error", ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void refreshFinancials() {
        if (paymentService == null || reservationId == null) {
            return;
        }

        paymentService.findReservation(reservationId).ifPresentOrElse(res -> {
            this.reservation = res;
            this.totalPaid = paymentService.calculateTotalPaid(res);
            this.currentBalance = paymentService.calculateBalance(res);

            totalPaidLabel.setText(formatCurrency(totalPaid));
            balanceLabel.setText(formatCurrency(currentBalance));
            updatePreview();
        }, () -> showAlert(Alert.AlertType.WARNING, "Reservation Not Found", "Unable to load reservation details."));
    }

    private void updatePreview() {
        try {
            BigDecimal entered = parseAmount(amountField.getText());
            PaymentType type = paymentTypeCombo.getValue();
            BigDecimal remaining;
            if (type == PaymentType.REFUND) {
                remaining = currentBalance.add(entered);
            } else {
                remaining = currentBalance.subtract(entered);
            }
            previewRemainingLabel.setText(formatCurrency(remaining.max(BigDecimal.ZERO)));
        } catch (Exception ex) {
            previewRemainingLabel.setText("-");
        }
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%,.2f", amount);
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
