package Controllers;

import Classes.*;
import Classes.XmlTag.Area;
import Classes.XmlTag.MeasuringChannel;
import Classes.XmlTag.MeasuringPoint;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.w3c.dom.*;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static Classes.Main.slash;
import static Classes.XML80020.*;
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
    public CheckBox checkBoxShowIntervals;
    public CheckBox checkBoxBatch;
    public Button btnReload;
    private List<File> fileList = new ArrayList<>();
    private boolean controlsEnabled = false;

    private Map<String, String> subjects;
    private long sumArray[][];
    private XML80020 currentXml;
    private Stage settingsStage;
    private Stage dataStage;
    // через эти переменные-контроллеры окон настроек и данных будем получать
    // доступ к элементам формы и методам класса
    private SettingsWindowController settingsWinControl;
    private DataWindowController dataWinControl;
    // инициализацмя переменной - окна "О программе"
    private Alert aboutWindow = new Alert(Alert.AlertType.INFORMATION);

    // массив кодов цветов для раскрашивания ими шапки с кодами
    private final static int[] colorNums = new int[] {11, 12, 20, 14, 62, 23, 25, 44, 28, 29, 45, 46, 52, 60, 49, 40};
    // массив списков ячеек Excel из строки с кодами ТИ в шапке, на которые срабатывает обходной выключатель
    private List<XSSFCell>[] extCodes;

    @FXML
    private void initialize() {
        try {
            // загружаем пиктограммы на кнопки
            Image imageXml = new Image("Resources/xls.png");
            btnMakeXLS.graphicProperty().setValue(new ImageView(imageXml));

            Image imageXls = new Image("Resources/xml.png");
            btnMake80020.graphicProperty().setValue(new ImageView(imageXls));

            Image imageReload = new Image("Resources/reload.png");
            btnReload.graphicProperty().setValue(new ImageView(imageReload));

            // при инициализации гл. окна программы создаем окно с настройками
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent settingsWin = fxmlLoader.load(getClass().getResource("/Resources/FXML/SettingsWindow.fxml").
                    openStream());
            // инициализируем переменную-контроллер, через нее будем получать доступ к элементам окна настроек
            settingsWinControl = fxmlLoader.getController();

            Scene settingsScene = new Scene(settingsWin);
            settingsStage = new Stage();
            settingsStage.setTitle("Настройки");
            settingsStage.setScene(settingsScene);
            settingsStage.setResizable(false);
            settingsStage.initModality(Modality.APPLICATION_MODAL);

            fxmlLoader = new FXMLLoader();
            Parent dataWin = fxmlLoader.load(getClass().getResource("/Resources/FXML/DataWindow.fxml").openStream());
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

            // создаем окно с прогресс-баром
            //progressBarCreate();
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
            if (!isNewSubject)
                applySubjectSettings();
        });

        textViewAIIS.textProperty().addListener(event ->{
            if (textViewAIIS.getText() != null && !textViewAIIS.getText().equals(""))
                btnDelAIIS.setDisable(false);
            else
                btnDelAIIS.setDisable(true);
        });
    }

    // метод возвращает список узлов value переданного measuringPoint-а и узла measuringChannel в нем
    private NodeList getValuesOfChannelNode (MeasuringPoint measuringPoint, MeasuringChannel measuringChannel) {
        NodeList valuesList = null;
            label:
            for (Area area : currentXml.getAreaList()) {
                for (Node measPointNode : area.getMeasPointNodeList()) {
                    if (measPointNode.getAttributes().getNamedItem("code").getNodeValue().
                            equals(measuringPoint.getCode())) {
                        for (int i = 0; i < measPointNode.getChildNodes().getLength(); i++) {
                            if (measPointNode.getChildNodes().item(i).getAttributes().
                                    getNamedItem("code").getNodeValue().equals(measuringChannel.getCode())) {
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
    private void fillMeasPointListView(XML80020 xmlClass) {
        ObservableList<MeasuringPoint> measPointObList = FXCollections.observableArrayList();
        // помещаем все measuringPoint-ы из всех area в measPointObList
        for (Area area: xmlClass.getAreaList())
            measPointObList.addAll(area.getMeasPointList());
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
            //btnDelAIIS.setDisable(false);
            radioButton30Min.setDisable(false);
            radioButton60Min.setDisable(false);
            checkBoxShowIntervals.setDisable(false);
            // ставим флаг доступности контролов в true
            controlsEnabled = true;
        }
        labelCountMeasPoints.setText(Integer.toString(measPointObList.size()));
        labelSelectedMeasPoints.setText(labelCountMeasPoints.getText());
        // загружаем настройки выбранных каналов, если такие найдутся
        if (!(xmlClass instanceof XML80025)) loadSubjectSettings();
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

    public void loadSubjectSettings(){
        textViewAIIS.setText("");
        this.subjects = new HashMap<>();
        Document xmlDoc = settingsWinControl.getSettingsXmlDoc();
        NodeList subjectNodeList = xmlDoc.getDocumentElement().getElementsByTagName("subject");
        //String senderINN = xml80020.getSender().getInn();
        String senderINN = currentXml.getSender().getInn();
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
        //fileChooser.setInitialDirectory(new File("D:\\Работа\\Макеты\\80020\\Калмыки")); /// удалить потом!
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
    // делаем доступными или недост. контролы для xml-файла класса 80020 в зависимости от класса макета
    // для 80025 он будут недоспупны
    private void setControlsDisabled (boolean enabled) {
        comboBoxAreaName.setDisable(enabled);
        btnReload.setDisable(enabled);
        btnSaveAIIS.setDisable(enabled);
        textViewAIIS.setDisable(enabled);
        btnMake80020.setDisable(enabled);
    }

    // показ данных из файла file (чтение из xml-файла, заполнение списка measuringpoint-ов и
    // measuringchannel-ов)
    private void displayAllXMLData (File file) {
        // в любой момент времени после загрузки xml-файла доступен только 1 объект класса 80020 или 80025
        // поэтому при вызове этого метода "обнуляем" оба, т.к. до этого этим объектом мог быть как
        // 80020, так и 80025. Далее в завис. от типа, с которого начинается имя файла
        // создаем объект нужного класса

        if (file.getName().startsWith("80020") || file.getName().startsWith("80040")) {
            XML80020 xml80020 = new XML80020(file);
            xml80020.loadDataFromXML();
            currentXml = xml80020;
            fillMeasPointListView(currentXml);
            setControlsDisabled(false);
        }
        if (file.getName().startsWith("80025")) {
            XML80025 xml80025 = new XML80025(file);
            xml80025.loadDataFromXML();
            currentXml = xml80025;
            fillMeasPointListView(currentXml);
            setControlsDisabled(true);
        }
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

    // создание файла excel (выгрузка данных)
    @FXML
    private void makeXLS(ActionEvent actionEvent) throws InterruptedException {
        // запоминаем тек. время
        Long startTime = new Date().getTime();
        // списки названий и кодов для выбранных ТИ
        List <String> mpNames = new ArrayList<>();
        List <String> mpCodes = new ArrayList<>();
        // создаем новую книгу Excel
        XSSFWorkbook workbook = new XSSFWorkbook();
        // создаем в книге 4 листа и задаем им цвет.
        workbook.createSheet(activeInput).setTabColor(new XSSFColor(java.awt.Color.green));
        workbook.createSheet(activeOutput).setTabColor(new XSSFColor(java.awt.Color.yellow));
        workbook.createSheet(reactiveInput).setTabColor(new XSSFColor(java.awt.Color.cyan));
        workbook.createSheet(reactiveOutput).setTabColor(new XSSFColor(java.awt.Color.orange));

        // заносим в списки названия и коды отмеченных ТИ
        for (Object object : measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint) object;
            if (measuringPoint.isSelected()) {
                mpNames.add(measuringPoint.getName());
                mpCodes.add(measuringPoint.getCode());
            }
        }
        // инициализируем массив списков кодов ТИ, на которые срабатывает обходной выключатель
        extCodes = new ArrayList[4];
        extCodes[0] = new ArrayList<>();
        extCodes[1] = new ArrayList<>();
        extCodes[2] = new ArrayList<>();
        extCodes[3] = new ArrayList<>();

        sumArray = new long[4][mpNames.size()];
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            createSheetHeader(workbook.getSheetAt(i), mpNames, mpCodes);
        }
        xmlDataToXls(workbook, 0);
        if (checkBoxBatch.isSelected()) { // если пакетная обработка, то перебираем все файлы
            for (int i = 1; i < fileList.size(); i++) {
                XML80020 prevXml = currentXml;
                if (fileList.get(i).getName().contains("80020") || fileList.get(i).getName().contains("80040")) {
                    currentXml = new XML80020(fileList.get(i));
                } else {
                    currentXml = new XML80025(fileList.get(i));
                }
                currentXml.loadDataFromXML();
                copySelectedProperty(prevXml, currentXml);
                xmlDataToXls(workbook, i);  // передаем workbook и индекс файла в списке файлов, индекс
                // нужен  для формирования номеров строк в excel
            }
        }

        sumArrayToSheet(workbook, sumArray);
        mpCodesToSheet(workbook);
        workbook.setActiveSheet(0);

        Long endTime = new Date().getTime();
        DateFormat formatter = new SimpleDateFormat("mm:ss");
        String execTime = formatter.format(endTime - startTime);
        messageWindow.showModalWindow("Завершено", "Время выполнения: " + execTime + ". Сохраните файл!",
                Alert.AlertType.INFORMATION);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить как");
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File(currentXml.getFile().getParent()));
        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                workbook.close();
                fos.close();
            }
            catch (IOException e) {
                messageWindow.showModalWindow("Ошибка", "Не удается сохранить файл " + file.getName() +
                        ". Возможно он открыт в другой программе.", Alert.AlertType.ERROR);
            }
        }
    }

    private void mpCodesToSheet(XSSFWorkbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            int rowNum = sheet.getLastRowNum() + 2;
            int k = 0;
            for (int j = 0; j < extCodes[i].size(); j++) {
                XSSFCell cell = extCodes[i].get(j);
                XSSFRow row = sheet.createRow(rowNum + k);
                XSSFCell codeCell = row.createCell(2);
                codeCell.setCellValue(cell.getStringCellValue());
                ExcelUtil.setCellFont(codeCell, IndexedColors.fromInt(cell.getCellStyle().getFont().getColor()),
                            false, false, true, false);
                k++;
                Hyperlink href = workbook.getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
                href.setAddress(cell.getReference());
                codeCell.setHyperlink(href);
            }
        }
    }

    private static void copySelectedProperty (XML80020 source, XML80020 dest) {
        for (int i = 0; i < source.getAreaList().size(); i++) {
            for (int j = 0; j < source.getAreaList().get(i).getMeasPointList().size(); j++) {
                boolean selected = source.getAreaList().get(i).getMeasPointList().get(j).isSelected();
                dest.getAreaList().get(i).getMeasPointList().get(j).setSelected(selected);
            }
        }
    }

    private void xmlDataToXls (XSSFWorkbook workbook, int fileNum) {
        int columnNum = 0;
        int pointNum = 0;
        for (int z = 0; z < currentXml.getAreaList().size(); z++) {
            for (int i = 0; i < currentXml.getAreaList().get(z).getMeasPointList().size(); i++) {
                if (currentXml.getAreaList().get(z).getMeasPointList().get(i).isSelected()) {

                    NodeList measChanNodeList = currentXml.getAreaList().get(z).getMeasPointNodeList().get(i).
                            getChildNodes();
                    for (int j = 0; j < measChanNodeList.getLength(); j++) {
                        String aliasChannelName = currentXml.getAreaList().get(z).getMeasPointList().get(i).
                                getMeasChannelList().get(j).getAliasName();
                        Element measChannel = (Element) measChanNodeList.item(j);
                        NodeList periodsNodeList = measChannel.getElementsByTagName("period");
                        Period30Min[] periods = new Period30Min[periodsNodeList.getLength()];
                        for (int k = 0; k < periodsNodeList.getLength(); k++) {
                            try {
                                periods[k] = new Period30Min();
                                periods[k].setStart(periodsNodeList.item(k).getAttributes().
                                        getNamedItem("start").getNodeValue());
                                periods[k].setEnd(periodsNodeList.item(k).getAttributes().
                                        getNamedItem("end").getNodeValue());
                                periods[k].setValue(Integer.parseInt(periodsNodeList.item(k).getChildNodes().
                                                item(0).getTextContent()));
                                Node statusAttr = periodsNodeList.item(k).getChildNodes().item(0).
                                        getAttributes().getNamedItem("status");
                                if (statusAttr != null)
                                    periods[k].setStatus(statusAttr.getNodeValue());
                                Node extStatusAttr = periodsNodeList.item(k).getChildNodes().item(0).
                                        getAttributes().getNamedItem("extendedstatus");
                                if (extStatusAttr != null) {
                                    periods[k].setExtendedstatus(extStatusAttr.getNodeValue());
                                    if (periods[k].getExtendedstatus().equals("1114"))
                                        periods[k].setParam1(periodsNodeList.item(k).getChildNodes().item(0).
                                                getAttributes().getNamedItem("param1").getNodeValue());
                                }
                            }
                            catch (NumberFormatException e) {
                                periods[k].setValue(0);
                            }
                        }
                        mcValuesToSheet(workbook, fileNum, currentXml.getDateTime().getDay(), aliasChannelName,
                                columnNum, periods);

                        switch (aliasChannelName) {
                            case activeInput : sumArray[0][pointNum] += Arrays.stream(periods).
                                    mapToLong(Period30Min::getValue).sum();
                                break;
                            case activeOutput : sumArray[1][pointNum] += Arrays.stream(periods).
                                    mapToLong(Period30Min::getValue).sum();
                                break;
                            case reactiveInput : sumArray[2][pointNum] += Arrays.stream(periods).
                                    mapToLong(Period30Min::getValue).sum();
                                break;
                            case reactiveOutput : sumArray[3][pointNum] += Arrays.stream(periods).
                                    mapToLong(Period30Min::getValue).sum();
                                break;
                        }
                    }
                    columnNum++;
                    pointNum++;
                }
            }
        }
    }
    //
    private void sumArrayToSheet(XSSFWorkbook workbook, long sumArray[][]) {
        for (int i = 0; i < sumArray.length; i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            int rowSum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowSum);
            for (int j = 0; j < sumArray[i].length; j++) {
                XSSFCell cell = (XSSFCell) row.createCell(2 + j);
                cell.setCellValue(sumArray[i][j]);
                ExcelUtil.setCellFont(cell, IndexedColors.BLACK, true, false, false, true);
            }
        }
    }

    // принамает книгу excel, поряд. номер файла, дату, алиасное имя канала, поряд. номер ТИ из списка, массив
    // значений в канале (48 получасовок) и формирует даныые в книге excel
    private void mcValuesToSheet(XSSFWorkbook workbook, int fileNum, String day, String aliasChannelName,
                                        int columnNum, Period30Min[] periods) {
        // по алиасному имени узнаем, в какой лист выводить данные
        XSSFSheet sheet = workbook.getSheet(aliasChannelName);
        String period;

        int z; // делитель временных интервалов: 1 (30 мин) или 2 (для 60 мин)
        if (radioButton30Min.isSelected())
            z = 1;
        else
            z = 2;

        for (int i = 0; i < periods.length; i++) {
           Row row = sheet.getRow(2 + (i / z) + (fileNum * (periods.length / z)));
           if (row == null)
               row = sheet.createRow(2 + (i / z) + (fileNum * (periods.length / z)));
           // в первый столбец каждой строки выводится дата
           row.createCell(0).setCellValue(Integer.parseInt(day));
           // если нужно выводить врем. интервал
           if (checkBoxShowIntervals.isSelected()) {
                period = "[" + periods[i].getStart().substring(0, 2) + "." + periods[i].getStart().substring(2, 4) +
                        " - " + periods[i].getEnd().substring(0, 2) + "." + periods[i].getEnd().substring(2, 4) + "]";
                row.createCell(1).setCellValue((Integer.toString(i + 1)) + " " + period);
            }
            // иначе
            else {
                row.createCell(1).setCellValue((i / z) + 1);
            }

           if (row.getCell(2 + columnNum) == null)
               row.createCell(2 + columnNum).setCellValue(periods[i].getValue() );
           else {
               int prevValue = (int )row.getCell(2 + columnNum).getNumericCellValue();
               row.getCell(2 + columnNum).setCellValue(periods[i].getValue() + prevValue);
           }

           // если инф. некоммер., то шрифт - красный толстый курсив
           if (periods[i].getStatus().equals("1"))
               ExcelUtil.setCellFont((XSSFCell)row.getCell(2 + columnNum), IndexedColors.RED, true, true,
                       false, false);

           // если есть extendedstatus (сработал обх. выключатель), то помечаем это значение
           if (periods[i].getExtendedstatus() != null && periods[i].getExtendedstatus().equals("1114")) {
               if (row.getCell(2 + columnNum).getCellComment() == null)
               ExcelUtil.setCellComment(row.getCell(2 + columnNum), periods[i].getParam1());
               XSSFRow rowCode = sheet.getRow(1);

               for (int j = 2; j < rowCode.getLastCellNum(); j++) {
                   if (rowCode.getCell(j).getStringCellValue().equals(periods[i].getParam1())) {
                       if (!extCodes[workbook.getSheetIndex(sheet)].contains(rowCode.getCell(j)))
                           extCodes[workbook.getSheetIndex(sheet)].add(rowCode.getCell(j));
                       int rowCodeColor = rowCode.getCell(j).getCellStyle().getFont().getColor();
                       ExcelUtil.setCellFont((XSSFCell)row.getCell(2 + columnNum), IndexedColors.fromInt(rowCodeColor),
                               true, false, false, false);
                       Cell cell = row.getCell(j);
                       if (cell == null) {
                           cell = row.createCell(j);
                       }
                       ExcelUtil.setCellColorAndFontColor((XSSFCell)cell, IndexedColors.fromInt(rowCodeColor),
                               IndexedColors.BLACK);
                       break;
                   }

               }
           }
        }
    }

    // метод выводит "шапку" из имени и кода ТИ на переданном листе
    private static void createSheetHeader(XSSFSheet sheet, List<String> names, List<String> codes) {
        // получаем по листу саму книгу excel
        XSSFWorkbook wb = sheet.getWorkbook();
        // создаем стиль ячейки: верт. и гориз. выравнивание по центру и тонкие границы
        CellStyle borderStyle = wb.createCellStyle();
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setAlignment(HorizontalAlignment.CENTER);
        borderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // создаем строки для имен  и кодов
        Row rowName = sheet.createRow(0);
        Row rowCode = sheet.createRow(1);
        // создаем ячейки
        rowName.createCell(0).setCellValue("Дата");
        rowName.createCell(1).setCellValue("Время");
        // устанавливаем стили для ячеек Дата и Время
        sheet.getRow(0).getCell(0).setCellStyle(borderStyle);
        sheet.getRow(0).getCell(1).setCellStyle(borderStyle);
        // создаем ячейки под "Дата" и "Время" и устанавливаем стили для них
        rowCode.createCell(0);
        rowCode.createCell(1);
        rowCode.getCell(0).setCellStyle(borderStyle);
        rowCode.getCell(1).setCellStyle(borderStyle);
        // объединяем ячейки "Дата" и "Время" с теми, что под ними
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));
        // создаем стиль для ячеек с именем ТИ
        CellStyle fillAndBorderStyle = wb.createCellStyle();
        fillAndBorderStyle.setBorderBottom(BorderStyle.THIN);
        fillAndBorderStyle.setBorderLeft(BorderStyle.THIN);
        fillAndBorderStyle.setBorderRight(BorderStyle.THIN);
        fillAndBorderStyle.setBorderTop(BorderStyle.THIN);
        fillAndBorderStyle.setAlignment(HorizontalAlignment.CENTER);
        fillAndBorderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        fillAndBorderStyle.setWrapText(true);
        XSSFFont font = wb.createFont();
        font.setFontHeight(9);
        fillAndBorderStyle.setFont(font);
        fillAndBorderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        fillAndBorderStyle.setFillPattern(FillPatternType.DIAMONDS);
        // создаем ячейки с именами и кодами ТИ и применяем стили к ним
        for (int i = 0; i < names.size(); i++) {
            sheet.setColumnWidth(2 + i, 4200);
            rowName.createCell(2 + i).setCellValue(names.get(i));
            rowName.setHeightInPoints(40);

            rowCode.createCell(2 + i).setCellValue(codes.get(i));
            rowName.getCell(2 + i).setCellStyle(fillAndBorderStyle);
            rowCode.getCell(2 + i).setCellStyle(borderStyle);
            ExcelUtil.setCellFont((XSSFCell) rowCode.getCell(2 + i),
                    IndexedColors.fromInt( colorNums[i % colorNums.length]),
                    false, false, false, true);
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
            autoSaveDir = null; // если автосохранение не стоит, то передаем значение null

        String outFileName;
        String outDirName;

        if (autoSaveDir != null) { // если обрабатываем только выделенный файл
            outDirName = autoSaveDir;
        }
        else { // если передали null-е значение, то сохраняем в ту же папку
            // в подпапку с именем класса макета (80020 или 80040)
            // если она не сущ., то создаем ее
            outDirName = currentXml.getFile().getParent() + slash + currentXml.getMessage().getMessageClass();
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
            String fileName = currentXml.getMessage().getMessageClass() + "_" +
                    senderINN +"_" +
                    currentXml.getDateTime().getDay() + "_" +
                    messNumber + "_" +
                    senderAIIS + ".xml";

            outFileName = outDirName + slash + fileName;
            try {
                currentXml.saveDataToXML(senderName, senderINN, areaName, areaINN, messVersion, messNumber,
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
            XML80020 xmlFirst = currentXml;
            for (File file : fileList) {
                if (num == Integer.MAX_VALUE) // если достигнуто макс. знач. Integer, то начинаем с 0
                    num = 0;
                num++; // увеличиваем на 1 номер message-а
                messNumber = Integer.toString(num);
                XML80020 xmlTemp = currentXml;
                currentXml = new XML80020(file);
                currentXml.loadDataFromXML();
                for (int i = 0; i < currentXml.getAreaList().size(); i++) {
                    Area area = currentXml.getAreaList().get(i);
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
                String fileName = currentXml.getMessage().getMessageClass() + "_" +
                        senderINN +"_" +
                        currentXml.getDateTime().getDay() + "_" +
                        messNumber + "_" +
                        senderAIIS + ".xml";

                outFileName = outDirName + slash + fileName;
                try {
                    currentXml.saveDataToXML(senderName, senderINN, areaName, areaINN, messVersion, messNumber,
                            newDLSavingTime, outFileName);
                }
                catch (TransformerException e) {
                    messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + outFileName +
                            " завершена неудачно!", Alert.AlertType.ERROR);
                    return;
                }
            }
            // текущему xml-файлу возращаем данные первого файла в списке (он является "выделенным" в списке)
            currentXml = xmlFirst;
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
            if (aiis.equals("") || name == null || name.equals("")) {
                XmlClass.messageWindow.showModalWindow("Внимание", "Имя контрагента и " +
                        "его код не должны быть пустыми!", Alert.AlertType.INFORMATION);
                return;
            }
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
        subjectNode.setAttribute("INN", currentXml.getSender().getInn());
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
        "действительно хотите удалить настройки для контрагента с кодом \"" +
                        textViewAIIS.getText() + "\"?", Alert.AlertType.CONFIRMATION);
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
