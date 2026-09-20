package app;

import app.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // asegurar base de datos e iniciar
        DatabaseConnection.init();

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/view/MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

        // cargar css
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        stage.setTitle("FIQA - Proyección de Aulas");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}