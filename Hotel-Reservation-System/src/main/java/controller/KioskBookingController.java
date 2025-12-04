package controller;

import app.Bootstrap;
import config.LoyaltyConfig;
import config.PricingConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.RoomType;
import service.RoomService;
import service.factory.RoomFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the stay details and room selection screen for the kiosk.
 */
public class KioskBookingController {
    private final RoomService roomService;
    private final PricingConfig pricingConfig;
    private final LoyaltyConfig loyaltyConfig;

    private final KioskFlowContext context;

    @FXML
    private DatePicker checkInPicker;
    @FXML
    private DatePicker checkOutPicker;
    @FXML
    private Spinner<Integer> adultsSpinner;
    @FXML
    private Spinner<Integer> childrenSpinner;
    @FXML
    private ComboBox<RoomType> roomTypeCombo;
    @FXML
    private ListView<String> suggestionsList;
    @FXML
    private Label occupancyLabel;
    @FXML
    private Label pricingLabel;
    @FXML
    private Label errorLabel;

    public KioskBookingController() {
        this(Bootstrap.getRoomService(),
                Bootstrap.getPricingConfig(), Bootstrap.getLoyaltyConfig(),
                KioskFlowContext.getInstance());
    }

    public KioskBookingController(RoomService roomService,
                                  PricingConfig pricingConfig,
                                  LoyaltyConfig loyaltyConfig,
                                  KioskFlowContext context) {
        this.roomService = roomService;
        this.pricingConfig = pricingConfig;
        this.loyaltyConfig = loyaltyConfig;
        this.context = context;
    }

    @FXML
    public void initialize() {
        adultsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 1));
        childrenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 4, 0));
        checkInPicker.setValue(LocalDate.now().plusDays(1));
        checkOutPicker.setValue(LocalDate.now().plusDays(2));
        loadAvailableRooms();
        refreshSuggestions();
    }

    @FXML
    private void refreshSuggestions() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        int adults = adultsSpinner.getValue();
        int children = childrenSpinner.getValue();

        context.setAdults(adults);
        context.setChildren(children);
        context.setCheckIn(checkIn);
        context.setCheckOut(checkOut);

        List<String> suggestionText = roomService.suggestRooms(adults, children, checkIn, checkOut)
                .stream()
                .map(this::formatSuggestion)
                .toList();
        suggestionsList.setItems(FXCollections.observableArrayList(suggestionText));
        occupancyLabel.setText("Guests: " + (adults + children));
        updatePricingPreview();
    }

    private String formatSuggestion(Object suggestion) {
        try {
            Method desc = suggestion.getClass().getDeclaredMethod("getDescription");
            Method types = suggestion.getClass().getDeclaredMethod("getRoomTypes");
            desc.setAccessible(true);
            types.setAccessible(true);
            Object description = desc.invoke(suggestion);
            Object roomTypes = types.invoke(suggestion);
            if (roomTypes instanceof List<?> list) {
                return description + " – " + String.join(", ", list.stream().map(Object::toString).toList());
            }
            return Objects.toString(description);
        } catch (Exception e) {
            return suggestion.toString();
        }
    }

    @FXML
    private void loadAvailableRooms() {
        List<RoomType> available = roomService.getAvailableRooms(checkInPicker.getValue(), checkOutPicker.getValue());
        ObservableList<RoomType> roomOptions = FXCollections.observableArrayList(available);
        if (roomOptions.isEmpty()) {
            roomOptions.add(createFallbackRoom());
        }
        roomTypeCombo.setItems(roomOptions);
        roomTypeCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getType() + " ($" + item.getBasePrice() + ")");
            }
        });
        roomTypeCombo.setButtonCell(roomTypeCombo.getCellFactory().call(null));
        if (!roomOptions.isEmpty()) {
            roomTypeCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void goToGuestDetails() throws IOException {
        errorLabel.setText("");
        Optional<String> validation = validateSelection();
        if (validation.isPresent()) {
            errorLabel.setText(validation.get());
            return;
        }

        RoomType selected = roomTypeCombo.getSelectionModel().getSelectedItem();
        context.getSelectedRooms().clear();
        context.getSelectedRooms().add(selected);
        context.setEstimatedTotal(updatePricingPreview());

        loadNext("/view/kiosk_guest_details.fxml");
    }

    private Optional<String> validateSelection() {
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (checkIn == null || checkOut == null) {
            return Optional.of("Please select check-in and check-out dates.");
        }
        if (!checkOut.isAfter(checkIn)) {
            return Optional.of("Check-out must be after check-in.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            return Optional.of("Check-in cannot be in the past.");
        }
        RoomType selected = roomTypeCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return Optional.of("Select at least one room type.");
        }
        int totalGuests = adultsSpinner.getValue() + childrenSpinner.getValue();
        if (totalGuests > selected.getCapacity()) {
            return Optional.of("Selected room cannot host " + totalGuests + " guests (capacity " + selected.getCapacity() + ")");
        }
        return Optional.empty();
    }

    private double updatePricingPreview() {
        RoomType selected = roomTypeCombo.getSelectionModel().getSelectedItem();
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();
        if (selected == null || checkIn == null || checkOut == null) {
            pricingLabel.setText("Pricing pending selection");
            return 0.0;
        }

        double total = 0.0;
        LocalDate cursor = checkIn;
        while (cursor.isBefore(checkOut)) {
            double nightly = selected.getBasePrice();
            nightly *= isWeekend(cursor) ? pricingConfig.getWeekendMultiplier() : pricingConfig.getWeekdayMultiplier();
            if (pricingConfig.isPeakSeason(cursor)) {
                nightly *= pricingConfig.getPeakSeasonMultiplier();
            }
            total += nightly;
            cursor = cursor.plusDays(1);
        }
        int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
        int earnedPoints = loyaltyConfig.calculatePointsEarned(total);
        pricingLabel.setText(String.format("%d nights total: $%.2f | Earn %d pts", nights, total, earnedPoints));
        return total;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private RoomType createFallbackRoom() {
        return RoomFactory.create(RoomType.Type.SINGLE, 120.0, 2);
    }

    private void loadNext(String resource) throws IOException {
        Stage stage = (Stage) checkInPicker.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        Parent root = loader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }
}
