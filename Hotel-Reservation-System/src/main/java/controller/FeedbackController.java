package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import service.FeedbackService;

/**
 * Collects guest feedback and stores it using the lightweight FeedbackService.
 */
public class FeedbackController {
    private final FeedbackService feedbackService;

    @FXML
    private Slider ratingSlider;
    @FXML
    private TextArea commentsArea;
    @FXML
    private TextField reservationField;
    @FXML
    private TextField guestField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button submitButton;

    public FeedbackController() {
        this(new FeedbackService());
    }

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @FXML
    private void submitFeedback() {
        int rating = (int) ratingSlider.getValue();
        Long reservationId = reservationField.getText().isBlank() ? null : Long.parseLong(reservationField.getText());
        feedbackService.submitFeedback(reservationId, guestField.getText(), rating, commentsArea.getText());
        statusLabel.setText("Thank you for your feedback!");
        commentsArea.clear();
        reservationField.clear();
    }
}
