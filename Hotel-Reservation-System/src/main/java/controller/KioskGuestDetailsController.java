package controller;

import app.Bootstrap;
import config.PricingConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Guest;
import service.BillingContext;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles Step 3 – guest details and add-ons.
 */
public class KioskGuestDetailsController {
    private final KioskFlowContext context;
    private final BillingContext billingContext;
    private final PricingConfig pricingConfig;

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private Label firstNameError;
    @FXML
    private Label lastNameError;
    @FXML
    private Label phoneError;
    @FXML
    private Label emailError;
    @FXML
    private CheckBox wifiCheck;
    @FXML
    private CheckBox breakfastCheck;
    @FXML
    private CheckBox parkingCheck;
    @FXML
    private CheckBox spaCheck;
    @FXML
    private Label addOnSummaryLabel;
    @FXML
    private Button nextButton;

    public KioskGuestDetailsController() {
        this(Bootstrap.getBillingContext(), Bootstrap.getPricingConfig(), KioskFlowContext.getInstance());
    }

    public KioskGuestDetailsController(BillingContext billingContext, PricingConfig pricingConfig, KioskFlowContext context) {
        this.billingContext = billingContext;
        this.pricingConfig = pricingConfig;
        this.context = context;
    }

    @FXML
    private void initialize() {
        Guest guest = context.getGuest();
        firstNameField.setText(guest.getFirstName() != null ? guest.getFirstName() : "");
        lastNameField.setText(guest.getLastName() != null ? guest.getLastName() : "");
        phoneField.setText(guest.getPhone() != null ? guest.getPhone() : "");
        emailField.setText(guest.getEmail() != null ? guest.getEmail() : "");

        List<String> existingAddOns = context.getAddOns();
        wifiCheck.setSelected(existingAddOns.contains("WiFi"));
        breakfastCheck.setSelected(existingAddOns.contains("Breakfast"));
        parkingCheck.setSelected(existingAddOns.contains("Parking"));
        spaCheck.setSelected(existingAddOns.contains("Spa"));

        wifiCheck.setOnAction(e -> updateAddOns());
        breakfastCheck.setOnAction(e -> updateAddOns());
        parkingCheck.setOnAction(e -> updateAddOns());
        spaCheck.setOnAction(e -> updateAddOns());

        firstNameField.textProperty().addListener((obs, o, n) -> validate());
        lastNameField.textProperty().addListener((obs, o, n) -> validate());
        phoneField.textProperty().addListener((obs, o, n) -> validate());
        emailField.textProperty().addListener((obs, o, n) -> validate());

        validate();
        updateAddOns();
    }

    @FXML
    private void backToDates() throws IOException {
        loadScene("/view/kiosk_booking_steps.fxml");
    }

    @FXML
    private void goToSummary() throws IOException {
        if (!validate()) {
            return;
        }

        Guest guest = context.getGuest();
        guest.setFirstName(firstNameField.getText().trim());
        guest.setLastName(lastNameField.getText().trim());
        guest.setPhone(phoneField.getText().trim());
        guest.setEmail(emailField.getText().trim());
        context.setGuest(guest);

        context.setAddOns(collectAddOns());
        if (context.getCheckIn() != null && context.getCheckOut() != null && !context.getSelectedRooms().isEmpty()) {
            KioskPricingHelper.BookingBreakdown breakdown = KioskPricingHelper.calculate(
                    context.getSelectedRooms(), context.getAddOns(), context.getCheckIn(), context.getCheckOut(), billingContext, pricingConfig);
            context.setEstimatedTotal(breakdown.total());
            context.setRoomSubtotal(breakdown.roomSubtotal());
            context.setAddOnSubtotal(breakdown.addOnSubtotal());
            context.setTax(breakdown.tax());
        }

        loadScene("/view/kiosk_summary.fxml");
    }

    private List<String> collectAddOns() {
        List<String> addOns = new ArrayList<>();
        if (wifiCheck.isSelected()) {
            addOns.add("WiFi");
        }
        if (breakfastCheck.isSelected()) {
            addOns.add("Breakfast");
        }
        if (parkingCheck.isSelected()) {
            addOns.add("Parking");
        }
        if (spaCheck.isSelected()) {
            addOns.add("Spa");
        }
        return addOns;
    }

    private boolean validate() {
        boolean valid = true;
        firstNameError.setText("");
        lastNameError.setText("");
        phoneError.setText("");
        emailError.setText("");

        if (firstNameField.getText().isBlank()) {
            firstNameError.setText("First name is required.");
            valid = false;
        }
        if (lastNameField.getText().isBlank()) {
            lastNameError.setText("Last name is required.");
            valid = false;
        }
        if (phoneField.getText().isBlank()) {
            phoneError.setText("Phone is required.");
            valid = false;
        }
        if (emailField.getText().isBlank()) {
            emailError.setText("Email is required.");
            valid = false;
        }

        nextButton.setDisable(!valid);
        return valid;
    }

    private void updateAddOns() {
        List<String> addOns = collectAddOns();
        long nights = 0;
        LocalDate in = context.getCheckIn();
        LocalDate out = context.getCheckOut();
        if (in != null && out != null) {
            nights = ChronoUnit.DAYS.between(in, out);
        }

        Map<String, Double> prices = BillingContext.getAvailableAddOns();
        double total = 0;
        for (String addOn : addOns) {
            Double price = prices.get(addOn);
            if (price == null) {
                continue;
            }
            boolean perNight = BillingContext.isAddOnPerNight(addOn);
            total += perNight && nights > 0 ? price * nights : price;
        }
        addOnSummaryLabel.setText(String.format("Add-ons subtotal: $%.2f", total));
    }

    private void loadScene(String resource) throws IOException {
        Stage stage = (Stage) nextButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Parent root = loader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }
}
