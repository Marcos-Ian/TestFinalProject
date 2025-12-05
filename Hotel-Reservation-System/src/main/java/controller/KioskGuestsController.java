package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Handles Step 1 of the kiosk flow: selecting adults and children.
 */
public class KioskGuestsController {
    private final KioskFlowContext context;

    @FXML
    private Spinner<Integer> adultsSpinner;
    @FXML
    private Spinner<Integer> childrenSpinner;
    @FXML
    private Label adultsErrorLabel;
    @FXML
    private Label childrenErrorLabel;
    @FXML
    private Button finishButton;
    @FXML
    private Button nextButton;

    public KioskGuestsController() {
        this(KioskFlowContext.getInstance());
    }

    public KioskGuestsController(KioskFlowContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, context.getAdults() > 0 ? context.getAdults() : 1));
        childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, Math.max(context.getChildren(), 0)));

        adultsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> validate());
        childrenSpinner.valueProperty().addListener((obs, oldVal, newVal) -> validate());
        validate();
    }

    @FXML
    private void goToDatesAndPlan() throws IOException {
        context.setAdults(adultsSpinner.getValue());
        context.setChildren(childrenSpinner.getValue());
        loadScene("/view/kiosk_booking_steps.fxml");
    }

    @FXML
    private void backToWelcome() throws IOException {
        loadScene("/view/kiosk_welcome.fxml");
    }

    @FXML
    private void onFinishGuestFlow() {
        try {
            Stage stage = (Stage) finishButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/kiosk_welcome.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validate() {
        adultsErrorLabel.setText("");
        childrenErrorLabel.setText("");

        boolean valid = true;
        if (adultsSpinner.getValue() == null || adultsSpinner.getValue() < 1) {
            adultsErrorLabel.setText("At least one adult is required.");
            valid = false;
        }

        if (childrenSpinner.getValue() == null || childrenSpinner.getValue() < 0) {
            childrenErrorLabel.setText("Children cannot be negative.");
            valid = false;
        }

        nextButton.setDisable(!valid);
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
