package controller;

import app.Bootstrap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import security.AdminUser;
import security.AuthenticationService;
import security.PasswordHasher;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles authentication for administrators.
 */
public class AdminLoginController {
    private final AuthenticationService authenticationService;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;

    public AdminLoginController() {
        this(Bootstrap.getAuthenticationService());
    }

    public AdminLoginController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @FXML
    private void attemptLogin() throws IOException {
        errorLabel.setText("");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        Optional<AdminUser> user = authenticationService.authenticate(username, password);
        if (user.isEmpty()) {
            // Fallback local admin for demo purposes using hashing
            String hash = PasswordHasher.hash("admin123");
            if (Objects.equals(username, "admin") && PasswordHasher.matches(password, hash)) {
                AdminUser admin = new AdminUser();
                admin.setUsername(username);
                admin.setPasswordHash(hash);
                admin.setRole(AdminUser.Role.ADMIN);
                user = Optional.of(admin);
            }
        }

        if (user.isPresent()) {
            authenticationService.setCurrentUser(user.get());
            loadDashboard(user.get());
        } else {
            errorLabel.setText("Invalid credentials");
        }
    }

    private void loadDashboard(AdminUser adminUser) throws IOException {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_dashboard.fxml"));
        loader.setControllerFactory(type -> type == AdminDashboardController.class
                ? new AdminDashboardController(adminUser)
                : createController(type));
        Parent root = loader.load();
        Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
    }

    private Object createController(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create controller: " + type, e);
        }
    }
}
