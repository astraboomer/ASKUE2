package Classes;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class TextWindow extends Alert {
    public TextWindow(AlertType alertType) {
        super(alertType);
    }

    public void showModalWindow (String title, String message, String text, Alert.AlertType messageType) {
        this.setAlertType(messageType);
        this.setTitle(title);
        this.setHeaderText(null);

        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        this.getDialogPane().setHeaderText(message);
        this.getDialogPane().setContent(expContent);
        this.showAndWait();
    }
}
