package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Guest;

import java.io.IOException;
import java.util.Objects;

/**
 * Collects guest contact information in the kiosk flow.
 */
public class KioskGuestDetailsController {
    private final KioskFlowContext context;

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button nextButton;

    public KioskGuestDetailsController() {
        this(KioskFlowContext.getInstance());
    }

    public KioskGuestDetailsController(KioskFlowContext context) {
        this.context = context;
    }

    @FXML
    private void goToAddons() throws IOException {
        errorLabel.setText("");
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
            errorLabel.setText("Guest name is required.");
            return;
        }

        Guest guest = context.getGuest();
        guest.setFirstName(firstNameField.getText().trim());
        guest.setLastName(lastNameField.getText().trim());
        guest.setPhone(phoneField.getText().trim());
        guest.setEmail(emailField.getText().trim());
        context.setGuest(guest);

        loadScene("/view/kiosk_addons.fxml");
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
