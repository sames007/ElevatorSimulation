package elevatorsim;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ElevatorSimulationApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(ElevatorSimulationApp.class.getResource("/elevatorsim/ui/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1180, 760);
        stage.setTitle("Elevator Simulation");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(660);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
