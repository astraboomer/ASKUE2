package Classes;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class MessageWindow extends Alert {
    public MessageWindow(AlertType alertType) {
        super(alertType);
    }

    public Optional<ButtonType> showModalWindow(String title, String message, Alert.AlertType messageType) {
        this.setAlertType(messageType);
        this.setTitle(title);
        this.setHeaderText(null);
        this.setContentText(message);
        return this.showAndWait();
    }
}
