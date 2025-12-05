package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import service.FeedbackService;

/**
 * Collects guest feedback and stores it using the FeedbackService.
 */
public class FeedbackController {
    private final FeedbackService feedbackService;

    @FXML
    private Slider ratingSlider;
    @FXML
    private TextArea commentsArea;
    @FXML
    private TextField emailField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button submitButton;

    public FeedbackController() {
        this(Bootstrap.getFeedbackService());
    }

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @FXML
    private void submitFeedback() {
        int rating = (int) ratingSlider.getValue();
        try {
            feedbackService.submitFeedback(emailField.getText(), rating, commentsArea.getText());
            statusLabel.setText("Thank you for your feedback!");
            commentsArea.clear();
            emailField.clear();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
}
