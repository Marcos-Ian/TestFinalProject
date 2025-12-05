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
    private TextField streetField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField provinceField;
    @FXML
    private TextField postalCodeField;
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
        wifiCheck.setOnAction(e -> updateAddOns());
        breakfastCheck.setOnAction(e -> updateAddOns());
        parkingCheck.setOnAction(e -> updateAddOns());
        spaCheck.setOnAction(e -> updateAddOns());

        firstNameField.textProperty().addListener((obs, o, n) -> validate());
        lastNameField.textProperty().addListener((obs, o, n) -> validate());
        phoneField.textProperty().addListener((obs, o, n) -> validate());
        emailField.textProperty().addListener((obs, o, n) -> validate());

        // Restore guest info if present
        model.Guest existing = context.getGuest();
        if (existing != null) {
            firstNameField.setText(existing.getFirstName() == null ? "" : existing.getFirstName());
            lastNameField.setText(existing.getLastName() == null ? "" : existing.getLastName());
            emailField.setText(existing.getEmail() == null ? "" : existing.getEmail());
            phoneField.setText(existing.getPhoneNumber() == null ? "" : existing.getPhoneNumber());
            streetField.setText(existing.getStreet() == null ? "" : existing.getStreet());
            cityField.setText(existing.getCity() == null ? "" : existing.getCity());
            provinceField.setText(existing.getProvince() == null ? "" : existing.getProvince());
            postalCodeField.setText(existing.getPostalCode() == null ? "" : existing.getPostalCode());
        }


        // Restore add-ons from context
        List<String> savedAddOns = context.getAddOns();
        if (savedAddOns != null) {
            wifiCheck.setSelected(savedAddOns.stream().anyMatch("WiFi"::equals));
            breakfastCheck.setSelected(savedAddOns.stream().anyMatch("Breakfast"::equals));
            parkingCheck.setSelected(savedAddOns.stream().anyMatch("Parking"::equals));
            spaCheck.setSelected(savedAddOns.stream().anyMatch("Spa"::equals));
        }

        validate();
        updateAddOns();
    }

    @FXML
    private void backToDates() throws IOException {
        loadScene("/view/kiosk_booking_steps.fxml");
    }

    @FXML
    private void goToSummary() throws IOException {
        // Clear previous errors
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        phoneError.setText("");

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String email     = emailField.getText().trim();
        String phone     = phoneField.getText().trim();

        boolean valid = true;

        // Basic first/last name checks
        if (firstName.isEmpty()) {
            firstNameError.setText("First name is required");
            valid = false;
        }
        if (lastName.isEmpty()) {
            lastNameError.setText("Last name is required");
            valid = false;
        }

        // Very simple email check – adjust if you already have something else
        if (email.isEmpty()) {
            emailError.setText("Email is required");
            valid = false;
        } else if (!email.contains("@")) {
            emailError.setText("Enter a valid email");
            valid = false;
        }

        // **Phone rule to match ReservationService**:
        // must be exactly 10 digits, no spaces or symbols
        if (phone.isEmpty()) {
            phoneError.setText("Phone is required");
            valid = false;
        } else if (!phone.matches("\\d{10}")) {
            phoneError.setText("Enter a 10-digit phone number");
            valid = false;
        }

        // If any field is invalid, stay on this screen
        if (!valid) {
            // optional: if you have a status label, give a general message
            // statusLabel.setText("Please fix the errors above before continuing.");
            return;
        }

        // If we reach here, everything is valid. Save to the flow context.
        Guest guest = context.getGuest();
        if (guest == null) {
            guest = new Guest();
        }
        guest.setFirstName(firstName);
        guest.setLastName(lastName);
        guest.setEmail(email);
        guest.setPhoneNumber(phone);
        guest.setStreet(streetField.getText());
        guest.setCity(cityField.getText());
        guest.setProvince(provinceField.getText());
        guest.setPostalCode(postalCodeField.getText());

        context.setGuest(guest);
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
        context.setAddOns(addOns);
        // Now move to the Review screen
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

    private String safeText(javafx.scene.control.TextField field) {
        String text = field.getText();
        return text == null ? "" : text;
    }

    private boolean validate() {
        boolean valid = true;

        String firstName = safeText(firstNameField).trim();
        String lastName  = safeText(lastNameField).trim();
        String email     = safeText(emailField).trim();
        String phone     = safeText(phoneField).trim();

        // clear previous errors
        firstNameError.setText("");
        lastNameError.setText("");
        emailError.setText("");
        phoneError.setText("");

        if (firstName.isEmpty()) {
            firstNameError.setText("First name is required");
            valid = false;
        }
        if (lastName.isEmpty()) {
            lastNameError.setText("Last name is required");
            valid = false;
        }
        if (email.isEmpty()) {
            emailError.setText("Email is required");
            valid = false;
        } else if (!email.contains("@")) {
            emailError.setText("Enter a valid email");
            valid = false;
        }

        if (phone.isEmpty()) {
            phoneError.setText("Phone is required");
            valid = false;
        } else if (!phone.matches("\\d{10}")) {
            phoneError.setText("Enter a 10-digit phone number");
            valid = false;
        }

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
