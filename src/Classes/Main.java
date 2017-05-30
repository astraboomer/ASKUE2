package Classes;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent mainWin = FXMLLoader.load(getClass().getResource("../FXML/MainWindow.fxml"));
        primaryStage.setTitle("АСКУЭ 1.0");
        Scene mainScene = new Scene(mainWin);
        primaryStage.setScene(mainScene);
        primaryStage.show();
        // при срабатывании закрытия окна для того, чтобы в опер. памяти не
        // осталось нитей приложения выполняется такой код
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
