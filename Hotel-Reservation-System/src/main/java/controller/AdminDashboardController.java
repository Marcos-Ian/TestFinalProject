package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Feedback;
import security.AdminUser;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import service.BillingContext;
import service.FeedbackService;
import service.LoyaltyService;
import service.ReservationService;
import service.WaitlistService;

import java.io.IOException;

/**
 * Hosts admin navigation and loads subordinate screens like search and edit.
 */
public class AdminDashboardController {
    private final AdminUser adminUser;
    private final ReservationService reservationService;
    private final LoyaltyService loyaltyService;
    private final BillingContext billingContext;
    private final WaitlistService waitlistService;
    private final FeedbackService feedbackService;

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label billingLabel;
    @FXML
    private Button searchButton;
    @FXML
    private Button createButton;
    @FXML
    private Button logoutButton;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab guestsTab;
    @FXML
    private Tab reservationsTab;
    @FXML
    private TableView<Feedback> feedbackTable;
    @FXML
    private TableColumn<Feedback, String> fbDateCol;
    @FXML
    private TableColumn<Feedback, String> fbEmailCol;
    @FXML
    private TableColumn<Feedback, Long> fbReservationCol;
    @FXML
    private TableColumn<Feedback, Integer> fbRatingCol;
    @FXML
    private TableColumn<Feedback, String> fbCommentsCol;

    public AdminDashboardController() {
        this(new AdminUser());
    }

    public AdminDashboardController(AdminUser adminUser) {
        this.adminUser = adminUser;
        this.reservationService = Bootstrap.getReservationService();
        this.loyaltyService = Bootstrap.getLoyaltyService();
        this.billingContext = Bootstrap.getBillingContext();
        this.waitlistService = new WaitlistService();
        this.feedbackService = Bootstrap.getFeedbackService();
    }

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + adminUser.getUsername());
        billingLabel.setText("Billing Strategy: " + billingContext.getStrategy().getClass().getSimpleName());
        if (mainTabPane != null && guestsTab != null) {
            mainTabPane.getSelectionModel().select(guestsTab);
        }

        initFeedbackTable();
        loadFeedback();
    }

    @FXML
    private void loadSearchView() {
        if (mainTabPane != null && reservationsTab != null) {
            mainTabPane.getSelectionModel().select(reservationsTab);
        }
    }



    @FXML
    private void loadEditView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_reservation_edit.fxml"));
            loader.setControllerFactory(type -> type == AdminReservationEditController.class
                    ? new AdminReservationEditController(reservationService, loyaltyService, waitlistService, feedbackService)
                    : createController(type));
            Parent view = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create/Modify Reservation");
            stage.setScene(new Scene(view));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load edit view", e);
        }
    }

    @FXML
    private void logout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_login.fxml"));
        Parent view = loader.load();
        logoutButton.getScene().setRoot(view);
    }

    private Object createController(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create controller: " + type, e);
        }
    }

    private void initFeedbackTable() {
        if (feedbackTable == null) {
            return;
        }

        if (fbDateCol != null) {
            fbDateCol.setCellValueFactory(cell -> {
                String date = cell.getValue().getCreatedAt() != null
                        ? cell.getValue().getCreatedAt().toLocalDate().toString()
                        : "";
                return new SimpleStringProperty(date);
            });
        }

        if (fbEmailCol != null) {
            fbEmailCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGuestEmail()));
        }

        if (fbReservationCol != null) {
            fbReservationCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(
                    cell.getValue().getReservation() != null ? cell.getValue().getReservation().getId() : null));
        }

        if (fbRatingCol != null) {
            fbRatingCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRating()));
        }

        if (fbCommentsCol != null) {
            fbCommentsCol.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getComments() != null ? cell.getValue().getComments() : ""));
        }
    }

    private void loadFeedback() {
        if (feedbackTable != null) {
            feedbackTable.setItems(FXCollections.observableArrayList(feedbackService.getAllFeedback()));
        }
    }
}
