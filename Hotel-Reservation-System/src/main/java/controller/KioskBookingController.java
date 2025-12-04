package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;

public class KioskBookingController {

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> adultsSpinner;
    @FXML private Spinner<Integer> childrenSpinner;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ListView<String> suggestionsList;
    @FXML private Label occupancyLabel;
    @FXML private Label pricingLabel;
    @FXML private Spinner<Integer> roomCountSpinner;
    @FXML private ListView<String> selectedRoomsList;



    @FXML
    private void initialize() {
        // Existing spinner setup
        if (adultsSpinner != null && adultsSpinner.getValueFactory() == null) {
            adultsSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1)
            );
        }
        if (childrenSpinner != null && childrenSpinner.getValueFactory() == null) {
            childrenSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0)
            );
        }

        // Room count spinner (1–5 rooms of each type, adjust as needed)
        if (roomCountSpinner != null && roomCountSpinner.getValueFactory() == null) {
            roomCountSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1)
            );
        }

        // Basic demo room types if you aren't pulling from RoomService yet
        if (roomTypeCombo != null && roomTypeCombo.getItems().isEmpty()) {
            roomTypeCombo.getItems().setAll(
                    "Standard Room",
                    "Deluxe Room",
                    "Suite",
                    "Family Room"
            );
            roomTypeCombo.setPromptText("Select a room type");
        }
    }


    @FXML
    private void refreshSuggestions() {
        // just to prove it works, later you plug real service logic
        suggestionsList.getItems().setAll("Suggestion 1", "Suggestion 2");
        occupancyLabel.setText("Guests: " +
                adultsSpinner.getValue() + " adults, " +
                childrenSpinner.getValue() + " children");
        pricingLabel.setText("Pricing: (sample)");
    }

    @FXML
    private void goBackToWelcome() {
        loadScene("/view/kiosk_welcome.fxml");
    }

    @FXML
    private void goToGuestDetails() {
        loadScene("/view/kiosk_guest_details.fxml");
    }

    private void loadScene(String fxmlPath) {
        try {
            Stage stage = (Stage) checkInPicker.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void addRoomToSelection() {
        String type = roomTypeCombo.getValue();
        if (type == null || type.isBlank()) {
            // You can also show an alert instead
            System.out.println("No room type selected");
            return;
        }

        int rooms = roomCountSpinner.getValue();
        int adults = adultsSpinner.getValue();
        int children = childrenSpinner.getValue();

        String item = String.format(
                "%d x %s  (%d adults, %d children)",
                rooms, type, adults, children
        );

        selectedRoomsList.getItems().add(item);

        // Optional: update summary labels
        occupancyLabel.setText("Guests: " + adults + " adults, " + children + " children");
        pricingLabel.setText("Pricing: multiple rooms (placeholder)");
    }

}
