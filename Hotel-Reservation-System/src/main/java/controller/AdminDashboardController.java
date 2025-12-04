package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
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
    private BorderPane contentPane;
    @FXML
    private Button searchButton;
    @FXML
    private Button createButton;
    @FXML
    private Button logoutButton;

    public AdminDashboardController() {
        this(new AdminUser());
    }

    public AdminDashboardController(AdminUser adminUser) {
        this.adminUser = adminUser;
        this.reservationService = Bootstrap.getReservationService();
        this.loyaltyService = Bootstrap.getLoyaltyService();
        this.billingContext = Bootstrap.getBillingContext();
        this.waitlistService = new WaitlistService();
        this.feedbackService = new FeedbackService();
    }

    @FXML
    public void initialize() throws IOException {
        welcomeLabel.setText("Welcome, " + adminUser.getUsername());
        billingLabel.setText("Billing Strategy: " + billingContext.getStrategy().getClass().getSimpleName());
        loadSearchView();
    }

    @FXML
    private void loadSearchView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_reservation_search.fxml"));
        loader.setControllerFactory(type -> type == AdminReservationSearchController.class
                ? new AdminReservationSearchController(
                reservationService,
                loyaltyService,
                billingContext,
                waitlistService,
                (Void ignored) -> {
                    try {
                        loadEditView();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        )
                : createController(type));
        Parent view = loader.load();
        contentPane.setCenter(view);
    }



    @FXML
    private void loadEditView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_reservation_edit.fxml"));
            loader.setControllerFactory(type -> type == AdminReservationEditController.class
                    ? new AdminReservationEditController(reservationService, loyaltyService, waitlistService, feedbackService)
                    : createController(type));
            Parent view = loader.load();
            contentPane.setCenter(view);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load edit view", e);
        }
    }

    @FXML
    private void logout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_login.fxml"));
        Parent view = loader.load();
        contentPane.getScene().setRoot(view);
    }

    private Object createController(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create controller: " + type, e);
        }
    }
}
