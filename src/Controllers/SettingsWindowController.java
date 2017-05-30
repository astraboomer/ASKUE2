package Controllers;


import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;


/**
 * Created by Сергей on 29.05.2017.
 */
public class SettingsWindowController {

    public void closeWindow(ActionEvent actionEvent) {
        Node node = ((Node)actionEvent.getSource());
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }
}
