package Controllers;

import Classes.XML80020;
import Classes.XmlClass;
import Classes.XmlTag.Area;
import Classes.XmlTag.MeasuringChannel;
import Classes.XmlTag.MeasuringPoint;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.*;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static Classes.XmlClass.alertWindow;

public class MainWindowController {

    public ListView filesListView;
    public Label labelCountXMLFiles;
    public ListView measPointListView;
    public Label labelCountMeasPoints;
    public Label labelSelectedMeasPoints;
    public Button btnSelectAll;
    public Button btnUnSelectAll;
    public ListView measChannelListView;
    public Label labelMeasPointCode;
    public Button btnMake80020;
    public Button btnMakeXLS;
    private List<File> fileList = new ArrayList<>();
    private XML80020 xml8020;
    private Stage settingsStage = new Stage();

    @FXML
    public void initialize() {
        try {
            // при инициализации гл. окна программы создаем окно с настройками
            Parent settingsWin = FXMLLoader.load(getClass().getResource("../FXML/SettingsWindow.fxml"));
            Scene settingsScene = new Scene(settingsWin);
            settingsStage = new Stage();
            settingsStage.setTitle("Настройки");
            settingsStage.setScene(settingsScene);
            settingsStage.setResizable(false);
            settingsStage.initModality(Modality.APPLICATION_MODAL);
        }
        catch (IOException e) {
            // выводим сообщение об шибке в случае неудачи
            alertWindow.setAlertType(Alert.AlertType.ERROR);
            alertWindow.setTitle("Ошибка");
            alertWindow.setHeaderText(null);
            alertWindow.setContentText(e.getMessage());
            alertWindow.showAndWait();
        }

        filesListView.getSelectionModel().selectedItemProperty().addListener(( ov, old_value,
                                                                               new_value) -> {
            if (new_value != null) {
                displayAllXMLData(fileList.get(filesListView.getSelectionModel().getSelectedIndex()));
            } else
                displayAllXMLData(fileList.get(0));
        });

        measPointListView.getSelectionModel().selectedItemProperty().addListener((observable, old_value,
                                                                                  new_value) -> {
            if (new_value != null) {
                MeasuringPoint measuringPoint = (MeasuringPoint) measPointListView.getSelectionModel().getSelectedItem();
                labelMeasPointCode.setText(measuringPoint.getCode());
                fillMeasChannelListView(measuringPoint);
                int countChannels = 0;
                for (MeasuringChannel measuringChannel: measuringPoint.getMeasChannelList()) {
                    countChannels++;
                    if (!measuringChannel.isCommercialInfo()) {
                        labelMeasPointCode.setTextFill(Color.RED);
                        break;
                    }
                    if (countChannels == measuringPoint.getMeasChannelList().size())
                        labelMeasPointCode.setTextFill(Color.BLACK);
                }
            } else
            {
                MeasuringPoint measuringPoint = (MeasuringPoint) measPointListView.getItems().get(0);
                labelMeasPointCode.setText(measuringPoint.getCode());
                fillMeasChannelListView(measuringPoint);
            }

        });

        measPointListView.setCellFactory(CheckBoxListCell.forListView(MeasuringPoint::selectedProperty,
                new StringConverter<MeasuringPoint>() {
                    @Override
                    public String toString(MeasuringPoint object) {
                        return object.getName();
                    }

                    @Override
                    public MeasuringPoint fromString(String string) {
                        return null;
                    }

                }));

        /*measChanelListView.setCellFactory(CheckBoxListCell.forListView(MeasuringChannel::selectedProperty,
                new StringConverter<MeasuringChannel>() {
                    @Override
                    public String toString(MeasuringChannel object) {
                        return object.getDesc();
                    }
                    @Override
                    public MeasuringChannel fromString(String string) {
                        return null;
                    }
                }));*/
    }

    // заполнение списка measuringpoint-ов
    public void fillMeasPointListView(XML80020 xml8020) {
        ObservableList<MeasuringPoint> measPointObList = FXCollections.observableArrayList();
        for (Area area: xml8020.getAreaList()){
            measPointObList.addAll(area.getMeasPointList());
        }
        measPointListView.setItems(measPointObList);
        // добавляем слушателя свойству выбора каждого элемента списка
        measPointObList.forEach(measPoint -> measPoint.selectedProperty().addListener((observable, wasSelected,
                                                                                       isSelected) -> {
            if (isSelected) {
                int countMeasPoints = Integer.parseInt(labelSelectedMeasPoints.getText());
                labelSelectedMeasPoints.setText(Integer.toString(countMeasPoints + 1));
            } else
            {
                int countMeasPoints = Integer.parseInt(labelSelectedMeasPoints.getText());
                labelSelectedMeasPoints.setText(Integer.toString(countMeasPoints - 1));
            }
        }));
        // после того, как список measuringpoint-ов заполнен делаем кнопки активными
        btnSelectAll.setDisable(false);
        btnUnSelectAll.setDisable(false);
        btnMake80020.setDisable(false);
        btnMakeXLS.setDisable(false);

        labelCountMeasPoints.setText(Integer.toString(measPointObList.size()));
        labelSelectedMeasPoints.setText(labelCountMeasPoints.getText());
        // выбираем первый элемент в списке measPointListView
        measPointListView.getSelectionModel().selectFirst();
        measPointListView.scrollTo(0);
        // получаем код measuringpoint-а
        labelMeasPointCode.setText(measPointObList.get(0).getCode());
    }

    // заполнение списка measuringchannel-ов переданного measuringPoint-а
    public void fillMeasChannelListView(MeasuringPoint measuringPoint) {
        ObservableList<MeasuringChannel> measChannelObList = FXCollections.observableArrayList();
        measChannelObList.addAll(measuringPoint.getMeasChannelList());
        measChannelListView.setItems(measChannelObList);
        // со всеми  measuringchannel-ами делаем:
        for (MeasuringChannel measuringChannel: measuringPoint.getMeasChannelList()) {
            // каждому measuringchannel-у (а он у нас наследуется от CheckBox)
            // даем название по его алиасному имени
            measuringChannel.setText(measuringChannel.getAliasName());
            // если канал имеет некомм. информацию
            if (!measuringChannel.isCommercialInfo()) {
                // помечаем этот канал курсивом в measChanelListView
                measuringChannel.setFont(Font.font("System", FontPosture.ITALIC, -1));
            }
        }
    }

    // выбор xml-файлов через диалог. окно
    public void selectXMLFiles(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("D:\\Работа\\Макеты\\80020\\Калмыки")); /// удалить потом!
        fileChooser.setTitle("Выберите файлы XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файлы XML", "*.xml"));
        // вводим лок.  переменную localFileList, которая инициализируется при каждом нажатии на кнопку
        // "Выбрать файлы XML".
        List<File>localFileList = fileChooser.showOpenMultipleDialog(new Stage());
        ObservableList<String> fileNames = FXCollections.observableArrayList();

        // если файлы выбраны (не нажата кнопка "отмена" диалога выбора файлов), т.е.
        // localFileList != null, то ей же присваиваем список корректных xml-файлов
        // если в localFileList не окажется файлов, то дальнейших действий не будет и
        // список файлов fileList остается с предыдущего удачного выбора файлов,
        // иначе fileList получает значение localFileList с коррект. списком файлов
        if (localFileList != null) {
            localFileList = XmlClass.validateXMLFiles(localFileList); // оставляем в списке только коррект. xml-файлы
            if (localFileList.size() > 0) { // если размер списка корректных файлов больше 0
                this.fileList = localFileList;
                this.fileList.sort(new Comparator<File>() { // далее сортируем файлы по именам
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                for (File file : this.fileList) {
                    fileNames.add(file.getAbsolutePath());
                }
                filesListView.setItems(fileNames);
                labelCountXMLFiles.setText(Integer.toString(fileNames.size()));

                filesListView.getSelectionModel().selectFirst();
            }
        }
    }

    // показ данных из файла file (чтение из xml-файла, заполнение списка measuringpoint-ов и
    // measuringchannel-ов)
    public void displayAllXMLData (File file) {
        xml8020 = new XML80020(file);
        xml8020.loadDataFromXML();
        fillMeasPointListView(xml8020);
        fillMeasChannelListView ((MeasuringPoint) measPointListView.getSelectionModel().getSelectedItem());
    }

    // выбор всех measuringpoint-ов (вызывается при нажатии на кнопку [ v ])
    public void selectAllMeasPoints(ActionEvent actionEvent) {
        for (Object measPoint: measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint)measPoint;
            measuringPoint.setSelected(true);
        }
    }

    // снятие выбора со всех measuringpoint-ов (вызывается при нажатии на кнопку [  ])
    public void unSelectAllMeasPoints(ActionEvent actionEvent) {
        for (Object measPoint: measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint)measPoint;
            measuringPoint.setSelected(false);
        }
    }

    // создание xml- файла (вызывается при нажатии на кнопку "Создать макет XML")
    public void makeXML(ActionEvent actionEvent) {
        xml8020.saveDataToXML();
    }

    // показ окна с настройками (вызывается при нажатии на пункт меню НАСТРОЙКИ)
    public void showSettingWindow(ActionEvent actionEvent) {
        settingsStage.show();
    }

    // закрытие приложения (вызывается при нажатии на пункт меню ВЫХОД)
    public void closeApplication(ActionEvent actionEvent) {
        // получаем окно по любому контролу (в данном случае по кнопке btnMake80020)
        // т.к. его нельзя получить из MenuItem в actionEvent
        Stage stage = (Stage) btnMake80020.getScene().getWindow();
        stage.close();
    }
}
