package controller;

import app.Bootstrap;
import config.PricingConfig;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.RoomType;
import service.BillingContext;
import service.RoomService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles Step 2 – selecting dates and a room plan.
 */
public class KioskBookingController {
    private final KioskFlowContext context;
    private final RoomService roomService;
    private final BillingContext billingContext;
    private final PricingConfig pricingConfig;

    @FXML
    private DatePicker checkInPicker;
    @FXML
    private DatePicker checkOutPicker;
    @FXML
    private Label checkInErrorLabel;
    @FXML
    private Label checkOutErrorLabel;
    @FXML
    private Label suggestionLabel;
    @FXML
    private RadioButton suggestedRadio;
    @FXML
    private RadioButton customRadio;
    @FXML
    private ComboBox<RoomType> roomTypeCombo;
    @FXML
    private Spinner<Integer> roomCountSpinner;
    @FXML
    private ListView<String> suggestionsList;
    @FXML
    private Label occupancyLabel;
    @FXML
    private Label customNoticeLabel;
    @FXML
    private Label estimateLabel;
    @FXML
    private Button nextButton;

    public KioskBookingController() {
        this(Bootstrap.getRoomService(), Bootstrap.getBillingContext(), Bootstrap.getPricingConfig(), KioskFlowContext.getInstance());
    }

    public KioskBookingController(RoomService roomService, BillingContext billingContext, PricingConfig pricingConfig, KioskFlowContext context) {
        this.roomService = roomService;
        this.billingContext = billingContext;
        this.pricingConfig = pricingConfig;
        this.context = context;
    }

    @FXML
    public void initialize() {
        int adults = context.getAdults();
        int children = context.getChildren();

        LocalDate checkIn = context.getCheckIn();
        LocalDate checkOut = context.getCheckOut();

        if (checkIn == null) {
            checkIn = LocalDate.now().plusDays(1);
        }
        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }

        checkInPicker.setValue(checkIn);
        checkOutPicker.setValue(checkOut);

        loadAvailableRooms();
        refreshSuggestions();

        checkInPicker.valueProperty().addListener((obs, oldV, newV) -> {
            context.setCheckIn(newV);
            loadAvailableRooms();
            refreshSuggestions();
        });

        checkOutPicker.valueProperty().addListener((obs, oldV, newV) -> {
            context.setCheckOut(newV);
            loadAvailableRooms();
            refreshSuggestions();
        });

        occupancyLabel.setText("Guests: " + (adults + children));
    }

    private void loadAvailableRooms() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        if (checkIn == null || checkOut == null) {
            roomTypeCombo.setItems(FXCollections.observableArrayList());
            return;
        }

        List<RoomType> available = roomService.getAvailableRooms(checkIn, checkOut);
        roomTypeCombo.setItems(FXCollections.observableArrayList(available));
        roomTypeCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getType().name());
            }
        });
        roomTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getType().name());
            }
        });
    }

    @FXML
    private void backToGuests() throws IOException {
        loadScene("/view/kiosk_step1_guests.fxml");
    }

    @FXML
    private void goToGuestDetails() throws IOException {
        if (validateDates()) {
            saveSelectionToContext();
            loadScene("/view/kiosk_guest_details.fxml");
        }
    }

    private void refreshSuggestions() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        int adults = context.getAdults();
        int children = context.getChildren();

        var items = roomService.suggestRooms(adults, children, checkIn, checkOut)
                .stream()
                .map(s -> s.getDescription() + " – " + String.join(", ", s.getRoomTypes()))
                .collect(java.util.stream.Collectors.toList());

        suggestionsList.setItems(javafx.collections.FXCollections.observableArrayList(items));
        occupancyLabel.setText("Guests: " + (adults + children));
    }

    private void onInputsChanged() {
        updateCustomControls();
        validateDates();
        updateSuggestionsAndEstimate();
    }

    private void updatePricingPreview() {
        updateSuggestionsAndEstimate();
    }

    private void updateCustomControls() {
        boolean custom = customRadio.isSelected();
        roomTypeCombo.setDisable(!custom);
        roomCountSpinner.setDisable(!custom);
        customNoticeLabel.setDisable(!custom);
    }

    private boolean validateDates() {
        checkInErrorLabel.setText("");
        checkOutErrorLabel.setText("");

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        boolean valid = true;

        if (checkIn == null) {
            checkInErrorLabel.setText("Check-in date is required.");
            valid = false;
        }

        if (checkOut == null) {
            checkOutErrorLabel.setText("Check-out date is required.");
            valid = false;
        }

        if (valid && !checkOut.isAfter(checkIn)) {
            checkOutErrorLabel.setText("Check-out must be after check-in.");
            valid = false;
        }

        nextButton.setDisable(!valid);
        return valid;
    }

    private void updateSuggestionsAndEstimate() {
        if (!validateDates()) {
            suggestionLabel.setText("Please choose valid dates to see suggestions.");
            estimateLabel.setText("Room cost estimate: --");
            return;
        }

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        List<RoomType> availableRooms = roomService.getAvailableRooms(checkIn, checkOut);

        if (availableRooms.isEmpty()) {
            suggestionLabel.setText("No rooms available for these dates.");
            nextButton.setDisable(true);
            return;
        }

        List<RoomType> suggested = buildSuggestedPlan(availableRooms);
        context.setSuggestedRooms(suggested);

        String summary = suggested.isEmpty()
                ? "No suggestion available."
                : String.format("Suggested: %d × %s based on your group size and availability.",
                suggested.size(), suggested.get(0).getType().name());
        suggestionLabel.setText(summary);

        List<RoomType> planRooms = suggestedRadio.isSelected() ? suggested : buildCustomPlan(availableRooms);
        if (planRooms == null || planRooms.isEmpty()) {
            estimateLabel.setText("Room cost estimate: --");
            nextButton.setDisable(true);
            return;
        }

        context.setUsingSuggestedPlan(suggestedRadio.isSelected());
        updateEstimate(planRooms, checkIn, checkOut);
    }

    private List<RoomType> buildSuggestedPlan(List<RoomType> availableRooms) {
        int totalGuests = context.getAdults() + context.getChildren();
        if (totalGuests <= 0) {
            return new ArrayList<>();
        }

        List<RoomType> sorted = availableRooms.stream()
                .sorted(Comparator.comparingInt(RoomType::getCapacity))
                .collect(Collectors.toList());

        List<RoomType> selection = new ArrayList<>();
        int remaining = totalGuests;
        for (RoomType room : sorted) {
            if (remaining <= 0) {
                break;
            }
            selection.add(room);
            remaining -= room.getCapacity();
        }

        if (remaining > 0 && !sorted.isEmpty()) {
            selection.add(sorted.get(sorted.size() - 1));
        }

        return selection;
    }

    private List<RoomType> buildCustomPlan(List<RoomType> availableRooms) {
        RoomType selectedType = roomTypeCombo.getValue();
        if (selectedType == null) {
            return null;
        }

        Map<RoomType.Type, RoomType> availableByType = availableRooms.stream()
                .collect(Collectors.toMap(RoomType::getType, r -> r, (a, b) -> a));

        RoomType match = availableByType.get(selectedType.getType());
        if (match == null) {
            return null;
        }

        int count = roomCountSpinner.getValue();
        List<RoomType> rooms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rooms.add(match);
        }
        customNoticeLabel.setText("If you choose your own room type and quantity, please review the booking policy carefully.");
        return rooms;
    }

    private void updateEstimate(List<RoomType> rooms, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        KioskPricingHelper.BookingBreakdown breakdown = KioskPricingHelper.calculate(rooms, context.getAddOns(), checkIn, checkOut, billingContext, pricingConfig);
        context.setSelectedRooms(rooms);
        context.setCheckIn(checkIn);
        context.setCheckOut(checkOut);
        context.setEstimatedTotal(breakdown.total());
        context.setRoomSubtotal(breakdown.roomSubtotal());
        context.setAddOnSubtotal(breakdown.addOnSubtotal());
        context.setTax(breakdown.tax());

        estimateLabel.setText(String.format("Room cost estimate: $%.2f for %d night(s), current plan.", breakdown.total(), nights));
        nextButton.setDisable(false);
    }

    private void populateRoomTypes() {
        List<RoomType> available = roomService.getAvailableRooms(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        roomTypeCombo.setItems(FXCollections.observableArrayList(available));
        roomTypeCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getType().name());
            }
        });
        roomTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getType().name());
            }
        });
    }

    private void saveSelectionToContext() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        List<RoomType> plan = context.isUsingSuggestedPlan() ? context.getSuggestedRooms() : buildCustomPlan(roomService.getAvailableRooms(checkIn, checkOut));
        if (plan != null) {
            context.setSelectedRooms(plan);
        }
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
