package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.FeedbackService;

import java.util.Objects;

/**
 * Collects guest feedback and stores it using the FeedbackService.
 */
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final Long reservationId;
    private final String defaultEmail;
    private final KioskFlowContext kioskContext;

    @FXML
    private Spinner<Integer> ratingSpinner;
    @FXML
    private TextArea commentsArea;
    @FXML
    private TextField emailField;
    @FXML
    private Button submitButton;

    public FeedbackController() {
        this(Bootstrap.getFeedbackService(), null, null, KioskFlowContext.getInstance());
    }

    public FeedbackController(FeedbackService feedbackService,
                              Long reservationId,
                              String defaultEmail,
                              KioskFlowContext kioskContext) {
        this.feedbackService = feedbackService;
        this.reservationId = reservationId != null ? reservationId
                : kioskContext != null ? kioskContext.getLastReservationId() : null;
        this.defaultEmail = defaultEmail != null ? defaultEmail
                : kioskContext != null && kioskContext.getGuest() != null ? kioskContext.getGuest().getEmail() : null;
        this.kioskContext = kioskContext;
    }

    @FXML
    private void initialize() {
        ratingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5));
        if (defaultEmail != null && !defaultEmail.isBlank()) {
            emailField.setText(defaultEmail);
        }
    }

    @FXML
    private void submitFeedback() {
        try {
            int rating = ratingSpinner.getValue();
            feedbackService.submitFeedback(
                    emailField.getText(),
                    reservationId,
                    rating,
                    commentsArea.getText()
            );

            showAlert(Alert.AlertType.INFORMATION, "Feedback submitted", "Thank you for your feedback!");
            if (kioskContext != null) {
                kioskContext.reset();
            }
            navigateHome();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Unable to submit feedback", ex.getMessage());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Unexpected error", "Something went wrong while submitting feedback.");
        }
    }

    private void navigateHome() throws Exception {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
