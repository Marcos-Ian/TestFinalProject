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
 * Primary navigation controller that routes users to kiosk, admin, or feedback flows.
 */
public class MainController {

    @FXML
    private Button kioskButton;

    @FXML
    private Button adminButton;

    @FXML
    private void openKiosk() throws IOException {
        loadScene("/view/kiosk_welcome.fxml");
    }

    @FXML
    private void openAdmin() throws IOException {
        loadScene("/view/admin_login.fxml");
    }

    @FXML
    private void openFeedback() throws IOException {
        loadScene("/view/feedback.fxml");
    }

    private void loadScene(String resource) throws IOException {
        Stage stage = (Stage) kioskButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Parent root = loader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }
}