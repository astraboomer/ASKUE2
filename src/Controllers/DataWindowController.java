package Controllers;

import Classes.XmlTag.Period;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DataWindowController {
    public Label labelMeasPoint;
    public Label labelMeasChannel;
    public TableColumn<Period, String> columnTime;
    public TableColumn<Period, String> columnValue;
    public TableColumn<Period, String> columnStatus;
    public TableView dataTableView;

    @FXML
    private void initialize(){
        columnTime.setCellValueFactory(new PropertyValueFactory<>("interval"));
        columnValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        columnStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }
}
