package Controllers;

import Classes.MeasuringData;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DataWindowController {
    public Label labelMeasPoint;
    public Label labelMeasChannel;
    public TableColumn<MeasuringData, String> columnTime;
    public TableColumn<MeasuringData, String> columnValue;
    public TableColumn<MeasuringData, String> columnStatus;
    public TableView dataTableView;

    @FXML
    private void initialize(){
        columnTime.setCellValueFactory(new PropertyValueFactory<>("timeInterval"));
        columnValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        columnStatus.setCellValueFactory(new PropertyValueFactory<>("typeInfo"));
    }
}
