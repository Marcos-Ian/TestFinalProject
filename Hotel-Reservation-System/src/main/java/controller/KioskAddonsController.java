package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Handles optional add-ons selection before showing the booking summary.
 */
public class KioskAddonsController {
    private final KioskFlowContext context;

    @FXML
    private CheckBox breakfastCheck;
    @FXML
    private CheckBox parkingCheck;
    @FXML
    private CheckBox spaCheck;
    @FXML
    private CheckBox lateCheckoutCheck;
    @FXML
    private Button nextButton;

    public KioskAddonsController() {
        this(KioskFlowContext.getInstance());
    }

    public KioskAddonsController(KioskFlowContext context) {
        this.context = context;
    }

    @FXML
    private void goToSummary() throws IOException {
        context.getAddOns().clear();
        if (breakfastCheck.isSelected()) {
            context.getAddOns().add("Breakfast");
        }
        if (parkingCheck.isSelected()) {
            context.getAddOns().add("Parking");
        }
        if (spaCheck.isSelected()) {
            context.getAddOns().add("Spa Access");
        }
        if (lateCheckoutCheck.isSelected()) {
            context.getAddOns().add("Late Checkout");
        }

        loadScene("/view/kiosk_summary.fxml");
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
