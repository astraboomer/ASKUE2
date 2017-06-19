package Classes;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.FileInputStream;

public class Main extends Application {
    public static final String slash = File.separator;
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent mainWin = FXMLLoader.load(getClass().getResource(".." + slash + "FXML" +
                    slash + "MainWindow.fxml"));
            primaryStage.setTitle("АСКУЭ");
            Scene mainScene = new Scene(mainWin);
            primaryStage.setScene(mainScene);
            FileInputStream iconStream = new FileInputStream(System.getProperty("user.dir") + slash +
                    "src" + slash + "Resources" + slash + "icon.png");
            Image imageIcon = new Image(iconStream);
            primaryStage.getIcons().add(imageIcon);

            primaryStage.show();
            // устанавливаем мин. допустимые ширину и высоту окна
            primaryStage.setMinWidth(primaryStage.getWidth());
            primaryStage.setMinHeight(primaryStage.getHeight());
            // при срабатывании закрытия окна для того, чтобы в опер. памяти не
            // осталось нитей приложения выполняется такой код
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });
        }
        catch (Exception e) {
            XmlClass.messageWindow.showModalWindow("Ошибка", e.getMessage() +
                    ". Программа будет закрыта.", Alert.AlertType.ERROR);
            Platform.exit();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
