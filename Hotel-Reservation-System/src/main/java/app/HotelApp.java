package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point that initializes the backend via {@link Bootstrap}
 * and loads the primary navigation view.
 */
public class HotelApp extends Application {

    @Override
    public void init() throws Exception {
        // Initialize backend services BEFORE JavaFX UI loads
        // This ensures Bootstrap services are ready when controllers are created
        Bootstrap.main(new String[]{});
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("Hotel Reservation System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Just launch JavaFX - init() will handle Bootstrap initialization
        launch(args);
    }
}