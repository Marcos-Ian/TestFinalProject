package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Landing screen for the self-service kiosk.
 */
public class KioskWelcomeController {

    @FXML
    private Button startButton;

    @FXML
    public void initialize() {
        KioskFlowContext.getInstance().reset();
    }

    @FXML
    private void startBooking() throws IOException {
        loadScene("/view/kiosk_step1_guests.fxml");
    }

    private void loadScene(String resource) throws IOException {
        Stage stage = (Stage) startButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Parent root = loader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }
}
