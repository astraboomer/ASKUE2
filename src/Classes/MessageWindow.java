package Classes;

import javafx.scene.control.Alert;

public class MessageWindow extends Alert {
    public MessageWindow(AlertType alertType) {
        super(alertType);
    }

    public void showModalWindow(String title, String message, Alert.AlertType messageType) {
        this.setAlertType(messageType);
        this.setTitle(title);
        this.setHeaderText(null);
        this.setContentText(message);
        this.showAndWait();
    }
}
