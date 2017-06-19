package Controllers;

import Classes.MeasuringData;
import Classes.XML80020;
import Classes.XmlClass;
import Classes.XmlTag.Area;
import Classes.XmlTag.MeasuringChannel;
import Classes.XmlTag.MeasuringPoint;
import Classes.XmlUtil;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.*;
import javafx.util.StringConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static Classes.Main.slash;
import static Classes.XmlClass.messageWindow;

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
    public TextField textViewAIIS;
    public ComboBox<String> comboBoxAreaName;
    public RadioButton radioButton30Min;
    public RadioButton radioButton60Min;
    public Button btnSaveAIIS;
    public Button btnDelAIIS;
    public CheckBox checkBoxDelColumns;
    public CheckBox checkBoxShowIntervals;
    public CheckBox checkBoxBatch;
    public Button btnReload;
    private List<File> fileList = new ArrayList<>();
    private boolean controlsEnabled = false;

    private Map<String, String> subjects;
    private XML80020 xml8020;
    private Stage settingsStage;
    private Stage dataStage;
    // через эти переменные-контроллеры окон настроек и данных будем получать
    // доступ к элементам формы и методам класса
    private SettingsWindowController settingsWinControl;
    private DataWindowController dataWinControl;
    private Alert aboutWindow = new Alert(Alert.AlertType.INFORMATION);

    @FXML
    private void initialize() {
        try {
            FileInputStream imageStream = new FileInputStream(System.getProperty("user.dir")+ slash +
                    "src" + slash + "Resources" + slash + "xls.png");
            Image imageXml = new Image(imageStream);
            btnMakeXLS.graphicProperty().setValue(new ImageView(imageXml));

            imageStream = new FileInputStream( System.getProperty("user.dir")+ slash +
                    "src" + slash +"Resources" + slash + "xml.png");
            Image imageXls = new Image(imageStream);
            btnMake80020.graphicProperty().setValue(new ImageView(imageXls));

            imageStream = new FileInputStream( System.getProperty("user.dir")+ slash +
                    "src" + slash +"Resources" + slash + "reload--.png");
            Image imageReload = new Image(imageStream);
            btnReload.graphicProperty().setValue(new ImageView(imageReload));


            // при инициализации гл. окна программы создаем окно с настройками
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent settingsWin = fxmlLoader.load(getClass().getResource(".." + slash + "FXML" +
                            slash + "SettingsWindow.fxml").openStream());
            // инициализируем переменную-контроллер, через нее будем получать доступ к элементам окна настроек
            settingsWinControl = fxmlLoader.getController();

            Scene settingsScene = new Scene(settingsWin);
            settingsStage = new Stage();
            settingsStage.setTitle("Настройки");
            settingsStage.setScene(settingsScene);
            settingsStage.setResizable(false);
            settingsStage.initModality(Modality.APPLICATION_MODAL);

            fxmlLoader = new FXMLLoader();
            Parent dataWin = fxmlLoader.load(getClass().getResource(".." + slash + "FXML" +
                    slash + "DataWindow.fxml").openStream());
            // инициализируем переменную-контроллер, через нее будем получать доступ к элементам окна настроек
            dataWinControl = fxmlLoader.getController();

            Scene dataScene = new Scene(dataWin);
            dataStage = new Stage();
            dataStage.setTitle("Данные");
            dataStage.setScene(dataScene);
            dataStage.setResizable(false);
            dataStage.initModality(Modality.APPLICATION_MODAL);
            dataStage.initStyle(StageStyle.UTILITY);

            // при инициализации гл. окна программы создаем окно "О программе"
            aboutWindow.setTitle("О программе");
            aboutWindow.setHeaderText("АСКУЭ 1.0");
            aboutWindow.setContentText("Для использования только в ПАО \"АЭСК\"" + System.lineSeparator() +
                    "Разработчик Ищенко С.А. " + System.lineSeparator() + "e-mail: astraboomer@hotmail.com");
        }
        catch (IOException e) {
            // выводим сообщение об шибке в случае неудачи
            messageWindow.showModalWindow("Ошибка", e.getMessage() + ". Программа будет закрыта.",
                    Alert.AlertType.ERROR);
            Platform.exit();
            System.exit(0);
        }

        // помещаем радио кнопки в одну группу выбора
        ToggleGroup toggleGroup = new ToggleGroup();
        radioButton30Min.setToggleGroup(toggleGroup);
        radioButton60Min.setToggleGroup(toggleGroup);

        // при выборе нового файла будет считываться информация из него (добавляется слушатель)
        filesListView.getSelectionModel().selectedItemProperty().addListener(( ov, old_value,
                                                                               new_value) -> {
            if (new_value != null) {
                displayAllXMLData(fileList.get(filesListView.getSelectionModel().getSelectedIndex()));
            }
        });

        // при нажатии на строку с measuringpoint-ом получаем его код и если
        // имеется некоммерч. информация красим красным label с кодом
        measPointListView.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable,
                                                                                  Object old_value, Object new_value) -> {
            if (new_value != null) { //если выбран новый елемент measPointListView-а
                MeasuringPoint measuringPoint = (MeasuringPoint) measPointListView.getSelectionModel().getSelectedItem();
                labelMeasPointCode.setText(measuringPoint.getCode());
                fillMeasChannelListView(measuringPoint);
                int countCommerChannels = 0; // счетчик кол-ва каналов с коммер. инф-цией

                // проверяем все measuringChannel-ы выбранного measuringPoint-а
                for (MeasuringChannel measuringChannel: measuringPoint.getMeasChannelList()) {

                    // в пункт контекс. меню каждого measuringChannel-а добавляем слушателя установки некомм. инф.,
                    // в котором меняется тип информации на некомм. и цвет кода текущ. measuringPoint-а
                    measuringChannel.getUnCommMenuItem().setOnAction(event -> {
                        NodeList valuesList = getValuesOfChannelNode(measuringPoint, measuringChannel);
                        measuringChannel.setCommercialInfo(false);
                        measuringChannel.setFont(Font.font("System", FontPosture.ITALIC, -1));
                        labelMeasPointCode.setTextFill(Color.RED);
                        if (valuesList != null)
                        for (int j = 0; j < valuesList.getLength(); j++) {
                            Element value = (Element) valuesList.item(j);
                            value.setAttribute("status", "1");
                        }
                    });
                    // в пункт контекс. меню каждого measuringChannel-а добавляем показа данных,
                    measuringChannel.getShowDataItem().setOnAction(event -> {
                        dataWinControl.labelMeasPoint.setText(measuringPoint.getName());
                        dataWinControl.labelMeasChannel.setText(measuringChannel.getAliasName());
                        // получаем список узлов value
                        NodeList valuesList = getValuesOfChannelNode(measuringPoint, measuringChannel);
                        ObservableList<MeasuringData> measDataList = FXCollections.observableArrayList();
                        for (int i = 0; i < valuesList.getLength(); i++) {
                            String start = valuesList.item(i).getParentNode().getAttributes().getNamedItem("start").
                                    getNodeValue();
                            String end = valuesList.item(i).getParentNode().getAttributes().getNamedItem("end").
                                    getNodeValue();
                            String value = valuesList.item(i).getTextContent();
                            Node statusNode = valuesList.item(i).getAttributes().getNamedItem("status");
                            String typeInfo;
                            if (statusNode != null) {
                                typeInfo = valuesList.item(i).getAttributes().getNamedItem("status").getNodeValue();
                            } else
                            {
                                typeInfo = "0";
                            }
                            String timeInterval = start.substring(0,2) + ":" + start.substring(2,4) +
                                    " - " + end.substring(0,2) + ":" + end.substring(2,4);
                            MeasuringData measuringData = new MeasuringData(timeInterval, value, typeInfo);
                            measDataList.add(measuringData);
                        }
                        dataWinControl.dataTableView.setItems(measDataList);
                        dataWinControl.dataTableView.scrollTo(0);
                        dataStage.show();
                    });

                    // добавляем слушателя на событие выбора/снятия галочки с канала
                    // если галочки сняты со всех каналов, то снимаем галочку с текущ. measuringPoint-а
                    measuringChannel.setOnAction(event -> {

                        int countDeselected = 0;
                        for (MeasuringChannel measChannel: measuringPoint.getMeasChannelList()) {
                            if (!measChannel.isSelected())
                                countDeselected++;
                        }
                        if (countDeselected == measuringPoint.getMeasChannelList().size())
                            measuringPoint.setSelected(false);
                    });

                    if (measuringChannel.isCommercialInfo())
                        countCommerChannels++; // считаем кол-во каналов с коммер. инф-цией
                }
                // в конце в завис-ти от того все каналы коммер. или не все
                // закрашиваем код  measuringPoint-а
                if (countCommerChannels == measuringPoint.getMeasChannelList().size())
                    labelMeasPointCode.setTextFill(Color.BLACK);
                else
                    labelMeasPointCode.setTextFill(Color.RED);
            }
        });

        // устанавливаем вид строк ListView как CheckBoxListCell и
        // указываем какой текст будет отображаться
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
        // добавляем слушателя к выбору значения имени субъекта
        comboBoxAreaName.valueProperty().addListener((observable, oldValue, newValue) -> {
            // при выборе субъекта получаем из map-ы subjects значение кода АИИС
            // флаг того, что в комбо-бокс введено новое значение субъекта, ранее там не
            // не содержавшееся
            boolean isNewSubject = true;
            for (Map.Entry<String, String> pair : subjects.entrySet()) {
                if (pair.getValue().equals(newValue)) {
                    textViewAIIS.setText(pair.getKey());
                    isNewSubject = false;
                    break;
                }
            }
            // если это новое значение, то не не нужно для него искать настройки
            // ищем настройки только при выборе значений из уже имеющихся в комбо-боксе
            if (!isNewSubject) applySubjectSettings();
        });
    }

    // метод возвращает список узлов value переданного measuringPoint-а и узла measuringChannel в нем
    private NodeList getValuesOfChannelNode (MeasuringPoint measuringPoint, MeasuringChannel measuringChannel) {
        NodeList valuesList = null;
        label:
        for (Area area: xml8020.getAreaList()) {
            for (Node measPointNode: area.getMeasPointNodeList()) {
                if (measPointNode.getAttributes().getNamedItem("code").getNodeValue().
                        equals(measuringPoint.getCode())) {
                    for (int i = 0; i < measPointNode.getChildNodes().getLength(); i++) {
                        if (measPointNode.getChildNodes().item(i).getAttributes().
                                getNamedItem("code").getNodeValue().equals(measuringChannel.getCode()))
                        {
                            Element measChannelNode = (Element) measPointNode.getChildNodes().item(i);
                            valuesList = measChannelNode.getElementsByTagName("value");
                            break label;
                        }
                    }
                }
            }
        }
        return valuesList;
    }

    // заполнение списка measuringpoint-ов
    private void fillMeasPointListView(XML80020 xml8020) {
        ObservableList<MeasuringPoint> measPointObList = FXCollections.observableArrayList();
        // помещаем все measuringPoint-ы из всех area в measPointObList
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
                for (MeasuringChannel measChannel: measPoint.getMeasChannelList()) {
                    measChannel.setSelected(true);
                    measChannel.setDisable(false);
                }
            } else
            {
                for (MeasuringChannel measChannel: measPoint.getMeasChannelList()) {
                    measChannel.setSelected(false);
                    measChannel.setDisable(true);
                }
                int countMeasPoints = Integer.parseInt(labelSelectedMeasPoints.getText());
                labelSelectedMeasPoints.setText(Integer.toString(countMeasPoints - 1));
            }

        }));
        // после того, как список measuringpoint-ов заполнен делаем кнопки активными
        // флаг доступности контролов установлен в false
        if (!controlsEnabled) {
            btnSelectAll.setDisable(false);
            btnUnSelectAll.setDisable(false);
            btnMake80020.setDisable(false);
            btnMakeXLS.setDisable(false);
            comboBoxAreaName.setDisable(false);
            btnReload.setDisable(false);
            textViewAIIS.setDisable(false);
            btnSaveAIIS.setDisable(false);
            btnDelAIIS.setDisable(false);
            radioButton30Min.setDisable(false);
            radioButton60Min.setDisable(false);
            checkBoxDelColumns.setDisable(false);
            checkBoxShowIntervals.setDisable(false);
            // ставим флаг доступности контролов в true
            controlsEnabled = true;
        }
        labelCountMeasPoints.setText(Integer.toString(measPointObList.size()));
        labelSelectedMeasPoints.setText(labelCountMeasPoints.getText());
        // загружаем настройки выбранных каналов, если такие найдутся
        loadSubjectSettings();
        // выбираем первый элемент в списке measPointListView
        measPointListView.getSelectionModel().selectFirst();
        measPointListView.scrollTo(0);
        // получаем код measuringpoint-а
        labelMeasPointCode.setText(measPointObList.get(0).getCode());
    }

    // заполнение списка measuringchannel-ов переданного measuringPoint-а
    private void fillMeasChannelListView(MeasuringPoint measuringPoint) {
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

    private void loadSubjectSettings(){
        textViewAIIS.setText("");
        this.subjects = new HashMap<>();
        Document xmlDoc = settingsWinControl.getSettingsXmlDoc();
        NodeList subjectNodeList = xmlDoc.getDocumentElement().getElementsByTagName("subject");
        String senderINN = xml8020.getSender().getInn();
        String measuringPointsNum = Integer.toString(measPointListView.getItems().size());
        ObservableList<String> areaNameObList = FXCollections.observableArrayList();
        String subjectINN;
        for (int i = 0; i < subjectNodeList.getLength(); i++) {
            subjectINN = subjectNodeList.item(i).getAttributes().getNamedItem("INN").getNodeValue();
            if (subjectINN.equals(senderINN) &&
                    subjectNodeList.item(i).getAttributes().getNamedItem("amount").getNodeValue().
                            equals(measuringPointsNum)) {
                subjects.put(subjectNodeList.item(i).getAttributes().getNamedItem("code").getNodeValue(),
                        subjectNodeList.item(i).getAttributes().getNamedItem("name").getNodeValue());
            }
        }
        areaNameObList.addAll(subjects.values());
        comboBoxAreaName.setItems(areaNameObList);
    }

    // выбор xml-файлов через диалог. окно
    @FXML
    private void selectXMLFiles(ActionEvent actionEvent) {
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
                checkBoxBatch.setDisable(false);
                filesListView.getSelectionModel().selectFirst();
            }
        }
    }

    // показ данных из файла file (чтение из xml-файла, заполнение списка measuringpoint-ов и
    // measuringchannel-ов)
    private void displayAllXMLData (File file) {
        xml8020 = new XML80020(file);
        xml8020.loadDataFromXML();
        fillMeasPointListView(xml8020);
    }

    // выбор всех measuringpoint-ов (вызывается при нажатии на кнопку [ v ])
    @FXML
    private void selectAllMeasPoints(ActionEvent actionEvent) {
        for (Object measPoint: measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint)measPoint;
            measuringPoint.setSelected(true);
        }
    }

    // снятие выбора со всех measuringpoint-ов (вызывается при нажатии на кнопку [  ])
    @FXML
    private void unSelectAllMeasPoints(ActionEvent actionEvent) {
        for (Object measPoint: measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint)measPoint;
            measuringPoint.setSelected(false);
        }
    }

    // создание xml- файла (вызывается при нажатии на кнопку "Создать макет XML")
    @FXML
    private void makeXML(ActionEvent actionEvent) {
        // получение значений элементов окна настроек через переменную settingsWinControl
        String senderName = settingsWinControl.textFieldName.getText();
        String senderINN = settingsWinControl.textFieldINN.getText();
        String areaName;
        String messNumber = "";
        if (comboBoxAreaName.getItems() != null)
            areaName = comboBoxAreaName.getSelectionModel().getSelectedItem();
        else
            areaName = "";
        String areaINN = textViewAIIS.getText();
        String messVersion = settingsWinControl.textFieldVersion.getText();
        String senderAIIS = settingsWinControl.textFieldAIIS.getText();
        String newDLSavingTime;
        String autoSaveDir;
        if (settingsWinControl.radioButtonWinter.isSelected())
            newDLSavingTime = "0";
        else
            newDLSavingTime = "1";
        if (settingsWinControl.checkBoxAutoSave.isSelected())
            autoSaveDir = settingsWinControl.textFieldSavePath.getText();
        else
            autoSaveDir = null; // если автосозранение не стоит, то передаем значение null

        String outFileName;
        String outDirName;

        if (autoSaveDir != null) { // если обрабатываем только выделенный файл
            outDirName = autoSaveDir;
        }
        else { // если передали null-е значение, то сохраняем в ту же папку
            // в подпапку с именем класса макета (80020 или 80040)
            // если она не сущ., то создаем ее
            outDirName = xml8020.getFile().getParent() + slash + xml8020.getMessage().getMessageClass();
            if (!Files.exists(Paths.get(outDirName)))
                new File (outDirName).mkdir();
        }

        if (!checkBoxBatch.isSelected()) { // если не пакетная обработка
            int num = Integer.parseInt(settingsWinControl.textFieldNumber.getText());
            if (num == Integer.MAX_VALUE) // если достигнуто макс. знач. Integer, то начинаем с 0
                num = 0;
            messNumber = Integer.toString(num + 1); // увеличиваем на 1 номер message-а
            settingsWinControl.textFieldNumber.setText(messNumber);
            // под этим именем сохраняем файл
            String fileName = xml8020.getMessage().getMessageClass() + "_" +
                    senderINN +"_" +
                    xml8020.getDateTime().getDay() + "_" +
                    messNumber + "_" +
                    senderAIIS + ".xml";

            outFileName = outDirName + slash + fileName;
            try {
                xml8020.saveDataToXML(senderName, senderINN, areaName, areaINN, messVersion, messNumber,
                        newDLSavingTime, outFileName);
            }
            catch (TransformerException e) {
                messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + outFileName +
                        " завершена неудачно!", Alert.AlertType.ERROR);
                return;
            }
        } else // если пакетная обработка
        {
            int num = Integer.parseInt(settingsWinControl.textFieldNumber.getText());
            // запоминаем данные первого xml-файла в списке
            XML80020 xmlFirst = xml8020;
            for (File file : fileList) {
                if (num == Integer.MAX_VALUE) // если достигнуто макс. знач. Integer, то начинаем с 0
                    num = 0;
                num++; // увеличиваем на 1 номер message-а
                messNumber = Integer.toString(num);
                XML80020 xmlTemp = xml8020;
                xml8020 = new XML80020(file);
                xml8020.loadDataFromXML();
                for (int i = 0; i < xml8020.getAreaList().size(); i++) {
                    Area area = xml8020.getAreaList().get(i);
                    for (int j = 0; j < area.getMeasPointList().size(); j++) {
                        MeasuringPoint measPoint = area.getMeasPointList().get(j);
                        measPoint.setSelected(xmlTemp.getAreaList().get(i).getMeasPointList().get(j).isSelected());
                        for (int k = 0; k < measPoint.getMeasChannelList().size(); k++) {
                            MeasuringChannel measChannel = measPoint.getMeasChannelList().get(k);
                            measChannel.setSelected(xmlTemp.getAreaList().get(i).getMeasPointList().get(j).
                                    getMeasChannelList().get(k).isSelected());
                        }
                    }
                }
                // под этим именем сохраняем файл
                String fileName = xml8020.getMessage().getMessageClass() + "_" +
                        senderINN +"_" +
                        xml8020.getDateTime().getDay() + "_" +
                        messNumber + "_" +
                        senderAIIS + ".xml";

                outFileName = outDirName + slash + fileName;
                try {
                    xml8020.saveDataToXML(senderName, senderINN, areaName, areaINN, messVersion, messNumber,
                            newDLSavingTime, outFileName);
                }
                catch (TransformerException e) {
                    messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + outFileName +
                            " завершена неудачно!", Alert.AlertType.ERROR);
                    return;
                }
            }
            // текущему xml-файлу возращаем данные первого файла в списке (он является "выделенным" в списке)
            xml8020 = xmlFirst;
        }
        messageWindow.showModalWindow("Выполнено", "Данные сохранены в папке " + outDirName,
                Alert.AlertType.INFORMATION);
        settingsWinControl.textFieldNumber.setText(messNumber);
        settingsWinControl.saveNumber(messNumber); // сразу сохраняем новый номер в файл настроек
    }

    // показ окна с настройками (вызывается при нажатии на пункт меню НАСТРОЙКИ)
    @FXML
    private void showSettingWindow(ActionEvent actionEvent) {
        settingsStage.show();
    }

    // закрытие приложения (вызывается при нажатии на пункт меню ВЫХОД)
    @FXML
    private void closeApplication(ActionEvent actionEvent) {
        // получаем окно по любому контролу (в данном случае по кнопке btnMake80020)
        // т.к. его нельзя получить из MenuItem в actionEvent
        Stage stage = (Stage) btnMake80020.getScene().getWindow();
        stage.close();
    }

    // показ окна "О программе"
    @FXML
    private void showAboutWindow(ActionEvent actionEvent) {
        aboutWindow.showAndWait();
    }

    @FXML
    private void saveAIIS(ActionEvent actionEvent) {
        Document xmlDoc = settingsWinControl.getSettingsXmlDoc();
        NodeList subjectNodeList = xmlDoc.getDocumentElement().getElementsByTagName("subject");
        String aiis = textViewAIIS.getText();
        String name = comboBoxAreaName.getValue();
        for (int i = 0; i < subjectNodeList.getLength(); i++) {
            if (subjectNodeList.item(i).getAttributes().getNamedItem("code").getNodeValue().equals(aiis)) {
                XmlClass.messageWindow.showModalWindow("Внимание", "Контрагент с таким кодом АИИС уже " +
                        "существует. Сохраните его под другим кодом!", Alert.AlertType.INFORMATION);
                return;
            }
            if (subjectNodeList.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                XmlClass.messageWindow.showModalWindow("Внимание", "Контрагент с таким именем уже " +
                        "существует. Сохраните его под другим названием!", Alert.AlertType.INFORMATION);
                return;
            }
        }

        int measuringPointNum = measPointListView.getItems().size();
        Element subjectNode = xmlDoc.createElement("subject");
        subjectNode.setAttribute("INN", xml8020.getSender().getInn());
        subjectNode.setAttribute("amount", Integer.toString(measuringPointNum));
        subjectNode.setAttribute("code", aiis);
        subjectNode.setAttribute("name", name);

        for (int i = 0; i < measuringPointNum; i++) {
            Object object = measPointListView.getItems().get(i);
            MeasuringPoint measuringPoint = (MeasuringPoint) object;
            Element measPointNode = xmlDoc.createElement("measuringpoint");
            measPointNode.setAttribute("code", measuringPoint.getCode());

            if (!measuringPoint.isSelected()) {
                subjectNode.appendChild(measPointNode);
            } else
            {
                // узнаем количество "выбранных" каналов у measuringPoint-а;
                long countSelectedChannel = measuringPoint.getMeasChannelList().stream().
                        filter(CheckBox::isSelected).count();
                if (countSelectedChannel != measuringPoint.getMeasChannelList().size()) {
                    for (MeasuringChannel measuringChannel: measuringPoint.getMeasChannelList()) {
                        if (!measuringChannel.isSelected())
                        {
                            Element measChannelNode = xmlDoc.createElement("measuringchannel");
                            measChannelNode.setAttribute("code", measuringChannel.getCode());
                            measPointNode.appendChild(measChannelNode);
                        }
                    }
                    subjectNode.appendChild(measPointNode);
                }
            }
        }

        xmlDoc.getDocumentElement().appendChild(subjectNode);
        try {
            // форматируем и сохраняем документ в xml-файл с кодировкой windows-1251
            XmlUtil.saveXMLDoc(xmlDoc, settingsWinControl.getFileName(), "windows-1251", true);
            messageWindow.showModalWindow("Сохранение", "Настройки выбора точек измерения и " +
                    "каналов успешно сохранены!", Alert.AlertType.INFORMATION);
            //loadSubjectSettings();
            subjects.put(aiis, name);
            comboBoxAreaName.getItems().add(name);
        }
        catch (TransformerException e) {
            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " +
                    settingsWinControl.getFileName() + " завершена неудачно!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void delAIIS(ActionEvent actionEvent) {
        Optional<ButtonType> result = messageWindow.showModalWindow("Удаление настроек выбора", "Вы "+
        "действительно хотите удалить настройки для \"" +
                        comboBoxAreaName.getValue() + "\"?", Alert.AlertType.CONFIRMATION);
        if (result.get() != ButtonType.OK) {
            return;
        }
        Document xmlDoc = settingsWinControl.getSettingsXmlDoc();
        NodeList subjectNodeList = xmlDoc.getDocumentElement().getElementsByTagName("subject");
        String aiis = textViewAIIS.getText();
        for (int i = 0; i < subjectNodeList.getLength(); i++) {
            if (subjectNodeList.item(i).getAttributes().getNamedItem("code").getNodeValue().equals(aiis)) {
                subjectNodeList.item(i).getParentNode().removeChild(subjectNodeList.item(i));
                break;
            }
        }
        try {
            // форматируем и сохраняем документ в xml-файл с кодировкой windows-1251
            XmlUtil.saveXMLDoc(xmlDoc, settingsWinControl.getFileName(), "windows-1251", true);
            loadSubjectSettings();
        }
        catch (TransformerException e) {
            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " +
                    settingsWinControl.getFileName() + " завершена неудачно!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void batchProcessing(ActionEvent actionEvent) {
        if (checkBoxBatch.isSelected())
        {
            filesListView.getSelectionModel().selectFirst();
            filesListView.setDisable(true);
        } else
        {
            filesListView.setDisable(false);
        }
    }
    //  обрабатываем нажатие кнопки "Обновить"
    @FXML
    private void reloadSubjectSettings(ActionEvent actionEvent) {
        if (comboBoxAreaName.getValue() != null)
            applySubjectSettings();
    }
    // применяем настройки выбора каналов и ТИ в соотв. с кодом АИИС в textViewAIIS
    private void applySubjectSettings() {
        // сначала по-умолчанию делаем "выбранными" все точки измерения и все каналы в них
        for (Object object: measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint) object;
            measuringPoint.setSelected(true);
            measuringPoint.getMeasChannelList().forEach(ch -> ch.setSelected(true));
        }
        // если есть загруженные из файла настройки для данного субъекта
        // то перебираем и в соот. с настроками делаем "выбранными" определенные ТИ и каналы
        if (subjects.size() > 0) {
            Document xmlDoc = settingsWinControl.getSettingsXmlDoc();
            NodeList subjectNodeList = xmlDoc.getDocumentElement().getElementsByTagName("subject");
            for (int i = 0; i < subjectNodeList.getLength(); i++) {
                if (subjectNodeList.item(i).getAttributes().getNamedItem("code").getNodeValue().
                        equals(textViewAIIS.getText())) {
                    NodeList measPointsNodeList = subjectNodeList.item(i).getChildNodes();
                    for (int j = 0; j < measPointsNodeList.getLength(); j++) {
                        String measPointCode = measPointsNodeList.item(j).getAttributes().getNamedItem("code").
                                getNodeValue();
                        for (Object object: measPointListView.getItems()) {
                            MeasuringPoint measuringpoint = (MeasuringPoint)object;

                            if (measuringpoint.getCode().equals(measPointCode)) {
                                int measChannelNum = measPointsNodeList.item(j).getChildNodes().getLength();
                                boolean hasChannels = measChannelNum > 0;
                                if (!hasChannels) {
                                    measuringpoint.setSelected(false);
                                } else
                                {
                                    for (int k = 0; k < measChannelNum; k++) {
                                        for (MeasuringChannel measuringChannel: measuringpoint.getMeasChannelList()) {
                                            if (measPointsNodeList.item(j).getChildNodes().item(k).getAttributes().
                                                    getNamedItem("code").getNodeValue().
                                                    equals(measuringChannel.getCode())) {
                                                measuringChannel.setSelected(false);
                                                break;
                                            }
                                        }

                                    }
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }

    }
}
