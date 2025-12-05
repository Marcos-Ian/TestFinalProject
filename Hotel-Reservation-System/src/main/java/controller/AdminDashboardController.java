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
import javafx.stage.Modality;
import javafx.stage.Stage;
import security.AdminUser;
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
}
