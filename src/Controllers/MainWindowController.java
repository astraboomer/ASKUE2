package Controllers;

import Classes.*;
import Classes.XmlTag.Area;
import Classes.XmlTag.MeasuringChannel;
import Classes.XmlTag.MeasuringPoint;
import Classes.XmlTag.Period;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.w3c.dom.*;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static Classes.Main.slash;
import static Classes.ServiceUtil.textWindow;
import static Classes.ServiceUtil.messageWindow;
import static Classes.XmlTag.MeasuringChannel.*;
import static Classes.XmlUtil.createXmlDoc;

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
    public Button btnMakeExcel;
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

    // масиив цветов для закрашивания названий ТИ в соответ. ячейках с теми же цветами
    private static final IndexedColors colors[] = {IndexedColors.LIGHT_GREEN, IndexedColors.LIGHT_YELLOW,
                                            IndexedColors.LIGHT_TURQUOISE, IndexedColors.LIGHT_CORNFLOWER_BLUE};

    // массив списков ячеек Excel из строки с кодами ТИ в шапке, на которые срабатывает обходной выключатель
    private List<SXSSFCell>[] extCodes;

    // строка с кодами ТИ в шапке. Нужна для доступа к ее ячейкам из других ячеек книги. Т.к. в книге типа
    // SXFFSWorkbook мы можем обращаться к строкам только в пределах заданного количества (при инициализации)
    private static SXSSFRow headerCodesRow;

    // стили ячеек
    private static CellStyle redTextStyle;
    private static CellStyle boldFontStyle;
    private static CellStyle boldAndBorderStyle;
    private static CellStyle boldAndFillStyle;

    // используется для запоминания последней выбранной директории
    private File lastOpenDir;
    // получаем пользов. директорую + Application Data/ASKUE (в ней будут храниться настройки программы)
    static final String appDataDir = System.getProperty("user.home") + slash + "Application Data" +
            slash + "ASKUE";

    @FXML
    private TextField textFieldOre;
    @FXML
    private TextField textFieldExcel;
    @FXML
    private TextField textFieldTimeZone;
    @FXML
    private TextField textField220;
    @FXML
    private TextField textField330;
    @FXML
    private TextField textFieldCalc;
    @FXML
    private TextField textFieldDel;
    @FXML
    private TextField textFieldSort;
    @FXML
    private ComboBox<String> comboBoxMonth;
    @FXML
    private ComboBox<String> comboBoxGMT;
    @FXML
    private ComboBox<String> comboBoxSubjectOre;
    @FXML
    private RadioButton radioBtnForward;
    @FXML
    private RadioButton radioBtnBack;
    @FXML
    private CheckBox checkBoxCompare;
    @FXML
    private CheckBox checkBoxTransferTime;
    @FXML
    private CheckBox checkBoxChangeSign;
    @FXML
    private CheckBox checkBoxConsumption;
    @FXML
    private TextField textFieldXml;
    @FXML
    private Button btnOpenXml;
    @FXML
    private Button btnOpenExcel;
    @FXML
    private Button btnMake50080;
    @FXML
    private Button btnMake51070;
    @FXML
    private Button btnSortFiles;
    @FXML
    private Button btnDelFiles;
    @FXML
    private Button btnCalc;
    @FXML
    private Button btnOpenCalc;
    @FXML
    private Button btnOpenDel;
    @FXML
    private Button btnOpenSort;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<Integer> spinnerYear;
    @FXML
    private Spinner<Integer> spinnerHour;
    @FXML
    private ProgressBar progressBar;

    // метод берет из папки ресурсов /Resources jar-а файл resource и сохраняет в файл fileName
    static void resourceToFile (String resource, String fileName) throws IOException {
        InputStream is = Main.class.getResourceAsStream(resource);
        byte[] buffer = new byte[1000];
        int count;
        FileOutputStream fos = new FileOutputStream(fileName);
        while ((count = is.read(buffer)) > 0) {
            fos.write(buffer, 0, count);
        }
        fos.close();
        is.close();
    }

    // инициализация контролов и загрузка ресурсов в Tab-ах
    private void initTab51070() {
        textFieldOre.setText(settingsWinControl.textFieldORE.getText());
        comboBoxMonth.getItems().addAll("январь", "февраль", "март", "апрель", "май", "июнь", "июль",
                "август", "сентябрь", "октябрь", "ноябрь", "декабрь");
        Calendar curDate = Calendar.getInstance();
        int monthIndex = curDate.get(Calendar.MONTH);
        int year = curDate.get(Calendar.YEAR);
        if (monthIndex == 0) {
            monthIndex = 11;
            year--;
        }
        else {
            monthIndex--;
        }

        spinnerYear.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 9999,
                year, 1));
        spinnerHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 23,
                2, 1));

        // ставим на datePicker год, месяц и 1-е число
        LocalDate localDate = LocalDate.of(year, monthIndex + 1, 1);
        datePicker.setValue(localDate);

        comboBoxMonth.getSelectionModel().select(monthIndex);
        comboBoxGMT.getItems().addAll("+", "-");
        comboBoxGMT.getSelectionModel().selectFirst();

        // объединяем в одну группу радиокнопки
        ToggleGroup toggleGroup = new ToggleGroup();
        radioBtnForward.setToggleGroup(toggleGroup);
        radioBtnBack.setToggleGroup(toggleGroup);

        // загружаем пиктограммы на кнопки
        Image imageXml = new Image("Resources/xml51070.png");
        btnMake51070.graphicProperty().setValue(new ImageView(imageXml));

        imageXml = new Image("Resources/xml50080.png");
        btnMake50080.graphicProperty().setValue(new ImageView(imageXml));

        // загружаем коды ОРЭ контрагентов из файла ORE.txt
        try {
            String fileName = appDataDir + slash + "ORE.txt";
            File oreFile = new File(fileName);
            List<String> subjectOre = Files.readAllLines(oreFile.toPath(), StandardCharsets.UTF_8);
            comboBoxSubjectOre.getItems().addAll(subjectOre);
        } catch (Exception e) {
            messageWindow.showModalWindow("Ошибка", "Не удалось загрузить коды ОРЭ контрагентов." +
                            " Проверьте доступность файла ORE.txt",
                    Alert.AlertType.ERROR);
        }

        // обработчики событий контролов

        checkBoxCompare.selectedProperty().addListener(event -> {
            if (checkBoxCompare.isSelected()) {
                textFieldXml.setDisable(false);
                btnOpenXml.setDisable(false);
            } else {
                textFieldXml.setDisable(true);
                btnOpenXml.setDisable(true);
            }
        });

        comboBoxSubjectOre.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                btnMake50080.setDisable(false);
            else
                btnMake50080.setDisable(true);
        });

        checkBoxTransferTime.selectedProperty().addListener(event -> {
            if (checkBoxTransferTime.isSelected()) {
                radioBtnForward.setDisable(false);
                radioBtnBack.setDisable(false);
                datePicker.setDisable(false);
                spinnerHour.setDisable(false);
            }
            else {
                radioBtnForward.setDisable(true);
                radioBtnBack.setDisable(true);
                datePicker.setDisable(true);
                spinnerHour.setDisable(true);
            }
        });

        btnOpenExcel.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл Excel");
            FileChooser.ExtensionFilter extFilter =
                    new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx", "*.xls");
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setInitialDirectory(lastOpenDir);
            File excelFile = fileChooser.showOpenDialog(new Stage());
            if (excelFile != null) {
                lastOpenDir = excelFile.getParentFile();
                textFieldExcel.setText(excelFile.getAbsolutePath());
                btnMake51070.setDisable(false);
            }
        });

        btnOpenXml.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл XML для сравнения");
            FileChooser.ExtensionFilter extFilter =
                    new FileChooser.ExtensionFilter("XML файлы", "*.xml");
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setInitialDirectory(lastOpenDir);
            File xmlFile = fileChooser.showOpenDialog(new Stage());
            if (xmlFile != null) {
                lastOpenDir = xmlFile.getParentFile();
                lastOpenDir = xmlFile.getParentFile();
                textFieldXml.setText(xmlFile.getAbsolutePath());
            }
        });

        btnMake51070.setOnAction(event -> make51070());
        btnMake50080.setOnAction(event -> make50080());

    }

    private void initTabAdditional() {
        // загружаем пиктограммы на кнопки
        Image imageXml = new Image("Resources/sort.png");
        btnSortFiles.graphicProperty().setValue(new ImageView(imageXml));

        imageXml = new Image("Resources/delete.png");
        btnDelFiles.graphicProperty().setValue(new ImageView(imageXml));

        imageXml = new Image("Resources/calc.png");
        btnCalc.graphicProperty().setValue(new ImageView(imageXml));

        btnOpenCalc.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл CSV");
            FileChooser.ExtensionFilter extFilter =
                    new FileChooser.ExtensionFilter("CSV файлы", "*.csv");
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setInitialDirectory(lastOpenDir);
            File excelFile = fileChooser.showOpenDialog(new Stage());
            if (excelFile != null) {
                lastOpenDir = excelFile.getParentFile();
                textFieldCalc.setText(excelFile.getAbsolutePath());
                btnCalc.setDisable(false);
            }
        });

        btnOpenDel.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Выберите папку с файлами XML");
            File dir = dirChooser.showDialog(new Stage());
            if (dir != null) {
                textFieldDel.setText(dir.getAbsolutePath());
                btnDelFiles.setDisable(false);
            }
        });

        btnOpenSort.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Выберите папку с файлами XML");
            File dir = dirChooser.showDialog(new Stage());
            if (dir != null) {
                textFieldSort.setText(dir.getAbsolutePath());
                btnSortFiles.setDisable(false);
            }
        });

        btnSortFiles.setOnAction(event -> {
            sortFiles(textFieldSort.getText());
            messageWindow.showModalWindow("Внимание", "Файлы рассортированы по папкам!",
                    Alert.AlertType.INFORMATION);
        });
        btnDelFiles.setOnAction(event -> {
            deleteFilesInDir(textFieldDel.getText());
            messageWindow.showModalWindow("Внимание", "Все файлы во всех подпапках удалены!",
                    Alert.AlertType.INFORMATION);
        });

        btnCalc.setOnAction(event -> {
            try {
                csvToXls(textFieldCalc.getText(), ',');
            } catch (IOException e) {
                messageWindow.showModalWindow("Ошибка", "Ошибка ввода/вывода. " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        });
    }

    // метод преобразует CVS-файл в XLS-файл и вызывает метод расчета потерь calcLosses()
    private void csvToXls(String fileName, char delimer) throws IOException {
        InputStream is = null;
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Лист 1");
        int i = 0;
        int j = 0;
        try {
            is = new BufferedInputStream(new FileInputStream(fileName));
            int data;
            byte[] tempBuffer = new byte[1];
            String str;
            StringBuilder stringBuilder = new StringBuilder();
            while ((data = is.read()) != -1) {      // пока читаются байты из файла
                if (data != (int) delimer && data != 13 && data != 10) {// если прочитанный символ не знак разделителя
                    // и не символ новой строки и возврата каретки
                    tempBuffer[0] = (byte) data;
                    // эта строка будет представлять собой 1 символ с кодировкой cp1251
                    str = new String(tempBuffer, "cp1251");
                    stringBuilder.append(str);
                }
                else {                                   // добавляем его в формируемую строку
                    if (data == 13) {                    // если символ новай строки, то строка из stringBuilder
                        i++;                             // будет выводиться с новой строки в excel и первого столбца
                        j = 0;
                        stringBuilder.setLength(0);
                        continue;
                    }
                    if (data == 10) {
                        continue;
                    }

                    Row row = sheet.getRow(i);
                    if (row == null) {
                        row = sheet.createRow(i);
                    }
                    byte[] bytes = stringBuilder.toString().getBytes();
                    str = new String(bytes);

                    // пытаемся перевести строку в числовой вид и заносим ее значение в ячейку excel
                    // если не получается, то заносим ее в ячейку excel как текст
                    try {
                        double numValue = Double.parseDouble(str);
                        row.createCell(j).setCellValue(numValue);
                    } catch (NumberFormatException e) {
                        row.createCell(j).setCellValue(str);
                    }
                    stringBuilder.setLength(0);
                    j++;
                }
            }
            calcLosses(workbook);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            workbook.close();
            if (is != null)
                is.close();
        }
    }

    private static void unZipFile(File file) throws  IOException {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos;
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                byte[] buffer = new byte[1000];
                int count;

                String fileNameInZip = zipEntry.getName();
                String saveDirName = file.getParent() + slash + fileNameInZip.substring(26, 43);
                File dir = new File (saveDirName);
                String saveFileName = file.getParent() + slash + fileNameInZip;

                if (!Files.exists(dir.toPath())) {
                    if (dir.mkdir())
                        saveFileName = saveDirName + slash + fileNameInZip;
                }
                else {
                    saveFileName = saveDirName + slash + fileNameInZip;
                }
                fos = new FileOutputStream(saveFileName);
                while ((count = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                zis.closeEntry();
            }
            zis.close();
    }

    // метод распаковывает все zip или gz файлы в директории dirName в эту же директорию и после этого удаляет архивы
    private static void sortFiles(String dirName) {
        File file = new File (dirName);
        File [] fileList = file.listFiles();
        if (fileList != null) {
            for (File myFile : fileList) {
                if (myFile.isFile())
                    try {
                        unZipFile(myFile);
                        myFile.delete();
                    } catch (IOException e) {
                        messageWindow.showModalWindow("Ошибка ввода/вывода", e.getMessage(), Alert.AlertType.ERROR);
                    }
            }
        }
    }

    // рекурсивный метод удаляет все файлы в директории dirName, включая файлы в во всех влож. директориях,
    // при этом оставляя сами директории нетронутыми
    private static void deleteFilesInDir(String dirName) {
        File file = new File (dirName);
        File [] fileList = file.listFiles();
        if (fileList != null) {
            for (File myFile : fileList) {
                if (myFile.isFile())
                        myFile.delete();
                else
                    deleteFilesInDir(myFile.getAbsolutePath());
            }
        }
    }

    // метод рассчитывает потери в книге excel workbook
    private void calcLosses(Workbook workbook) {
        Sheet newSheet = workbook.createSheet("Расчет потерь");
        workbook.setSheetOrder("Расчет потерь", 0);
        Sheet sheet = workbook.getSheetAt(1);

        // узнаем кол-во строк и стобцов в листе
        int rowNum = sheet.getLastRowNum() + 1;
        int cellNum = sheet.getRow(rowNum - 1).getLastCellNum();
        Row headerRow = newSheet.createRow(0);

        // коэф-ты в расчете формулы потерь
        final double reactR = 0.289;
        final int linU = 6;
        // значение потери эл/энергии за интервал 1 час в конкрет. ТИ
        double wLoss;
        // будет использоваться для округления до 3-х знаков после запятой
        BigDecimal bigDecimal;

        // выводим заголовок нового листа из названий ТИ
        for (int i = 0; i < (cellNum - 2) / 2; i++) {
            String pointName = sheet.getRow(4).getCell(2 + 2 * i).getStringCellValue();
            headerRow.createCell(2 + i).setCellValue(pointName);
        }
        for (int i = 0; i < (rowNum - 12) / 2; i++) {
            Row row1 = sheet.getRow(12 + i * 2);
            Row row2 = sheet.getRow(12 + i * 2 + 1);
            String date = row2.getCell(0).getStringCellValue();
            newSheet.createRow(1 + i).createCell(0).setCellValue(date);
            int hour = (int) row2.getCell(1).getNumericCellValue() / 2;
            newSheet.getRow(1 + i).createCell(1).setCellValue(hour);
            for (int j = 0; j < (cellNum - 2) / 2; j++) {
                double a1;
                double a2;
                double r1;
                double r2;
                // если считываемое поле не содержит число, то
                // переменные будут равны 0
                try {
                    a1 = row1.getCell(2 + j * 2).getNumericCellValue();
                } catch (IllegalStateException e) {
                    a1 = 0;
                }
                try {
                    a2 = row2.getCell(2 + j * 2).getNumericCellValue();
                } catch (IllegalStateException e) {
                    a2 = 0;
                }
                try {
                    r1 = row1.getCell(2 + j * 2 + 1).getNumericCellValue();
                } catch (IllegalStateException e) {
                    r1 = 0;
                }
                try {
                    r2 = row2.getCell(2 + j * 2 + 1).getNumericCellValue();
                } catch (IllegalStateException e) {
                    r2 = 0;
                }
                // формула расчета
                wLoss = 2 * (reactR / (linU * linU * 1000)) * (a1 * a1 + r1 * r1 + a2 * a2 + r2 * r2);
                // округляем до 3 знаков после запятой
                bigDecimal = new BigDecimal(wLoss).setScale(3, BigDecimal.ROUND_HALF_UP);
                // и выводим в ячейку EXCEL
                newSheet.getRow(1 + i).createCell(2 + j).setCellValue(bigDecimal.doubleValue());
            }
        }
        workbook.setActiveSheet(0);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить как");
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("Excel файлы", "*.xls");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            try {
                if (!file.getName().endsWith(".xls")) {
                    String newFileName = file.getAbsolutePath() + ".xls";
                    file = new File(newFileName);
                }
                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                fos.close();
            } catch (IOException e) {
                messageWindow.showModalWindow("Ошибка", "Не удается сохранить файл " + file.getName() +
                        ". Возможно он открыт в другой программе.", Alert.AlertType.ERROR);
            }
        }
    }

    // метод создает файл XML в формате 50080
    private void make50080() {
        // если поля 220 и 330 не заполнены, сразу завершаем метод
        if (textField220.getText().equals("") || textField330.getText().equals("")) {
            messageWindow.showModalWindow("Внимание", "Значения полей 330 кВ и 220 кВ не " +
                    "должны быть пустыми", Alert.AlertType.WARNING);
            return;
        }
        try {
            // создаем новый DOM-документ XML с соответ. узлами и атрибутами
            Document xmlDoc = createXmlDoc();
            Element root = xmlDoc.createElement("message");
            xmlDoc.appendChild(root);

            Date curDate = new Date();
            DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String createdTime = formatter.format(curDate);
            String offset = "GMT" + getTimeZoneOffset(curDate);
            String messageId = getUId();
            String sender = settingsWinControl.textFieldINN.getText();
            String kpokod = "";

            root.setAttribute("class", "50080");
            root.setAttribute("version", "1");
            root.setAttribute("sender", sender);
            root.setAttribute("created", createdTime + offset);
            root.setAttribute("id", messageId);
            root.setAttribute("kpokod", kpokod);

            Element adjacent = xmlDoc.createElement("adjacent");
            adjacent.setAttribute("code-from", textFieldOre.getText());
            adjacent.setAttribute("code-to", comboBoxSubjectOre.getValue());

            Element flow = xmlDoc.createElement("flow");
            String mon = Integer.toString(comboBoxMonth.getSelectionModel().getSelectedIndex() + 1);
            if (mon.length() < 2)
                mon = "0" + mon;
            String month = spinnerYear.getValue().toString() + mon;
            flow.setAttribute("month", month);

            Element power330 = xmlDoc.createElement("power330");
            Element power220 = xmlDoc.createElement("power220");
            power330.setTextContent(textField330.getText());
            power220.setTextContent(textField220.getText());

            flow.appendChild(power330);
            flow.appendChild(power220);
            adjacent.appendChild(flow);
            root.appendChild(adjacent);

            // под этим именем будем сохранять
            String outFileName = "d50080" + sender + createdTime + ".xml";
            FileChooser fileChooser = new FileChooser();
            // задаем имя XML файла в диалоге сохранения
            fileChooser.setInitialFileName(outFileName);
            FileChooser.ExtensionFilter extFilter =
                    new FileChooser.ExtensionFilter("XML файлы", "*.xml");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showSaveDialog(new Stage());
            // если имя выбрано файл
            if (file != null) {
                XmlUtil.saveXMLDoc(xmlDoc, file.getAbsolutePath(), "windows-1251", true);
            }

        } catch (Exception e) {
            messageWindow.showModalWindow("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // метод возвращает уник. 32-значный id в верхнем регистре
    private String getUId() {
        String uId = UUID.randomUUID().toString().replaceAll("-", "");
        return uId.toUpperCase();
    }

    // метод возвращает сдвиг часового пояса GMT из настроек даты компа
    private String getTimeZoneOffset(Date date) {
        TimeZone timeZone = TimeZone.getDefault();
        int tzOffset = timeZone.getOffset(date.getTime()) / 3600000;
        String GMTSign = Integer.toString(tzOffset);
        if (tzOffset > 0)
            GMTSign = "+" + GMTSign;
        return GMTSign;
    }

    // метод создает файл XML в формате 50080
    private void make51070() {
        // если галочка сравнения с др. файлом xml стоит, но этот файл не выбран, то выходим из метода
        if (checkBoxCompare.isSelected() && textFieldXml.getText().equals("")) {
            messageWindow.showModalWindow("Внимание", "Не выбран файл XML для сравнения",
                    Alert.AlertType.WARNING);
            return;
        }
        String excelFileName = textFieldExcel.getText();
        Workbook workbook;
        // пытаемся создать workbook из xls-файла (excel 2003)
        try {
            workbook = new HSSFWorkbook(new FileInputStream(excelFileName));
        } catch (IOException e) {
            messageWindow.showModalWindow("Ошибка", "Не удается открыть " + excelFileName + "." +
                    "Проверьте доступность файла", Alert.AlertType.ERROR);
            return;
        }
        // обрабатываем ошибку, если выбран xlsx-файл (excel 2007 и старше)
        catch (org.apache.poi.poifs.filesystem.OfficeXmlFileException e1) {
            try {
                // создаем workbook из xlsx-файла
                workbook = new XSSFWorkbook(excelFileName);
            } catch (IOException e2) {
                messageWindow.showModalWindow("Ошибка", "Не удается открыть " + excelFileName + "." +
                        "Проверьте доступность файла", Alert.AlertType.ERROR);
                return;
            }
        }

        try {
            // создаем новый DOM-документ XML с соответ. узлами и атрибутами
            Document xmlDoc = createXmlDoc();
            Element root = xmlDoc.createElement("package");
            xmlDoc.appendChild(root);

            Date curDate = new Date();
            DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String createdTime = formatter.format(curDate);
            String offset = "GMT" + getTimeZoneOffset(curDate);
            String messageId = getUId();
            String sender = settingsWinControl.textFieldINN.getText();

            root.setAttribute("class", "51070");
            root.setAttribute("version", "1");
            root.setAttribute("sender", sender);
            root.setAttribute("created", createdTime + offset);
            root.setAttribute("id", messageId);

            // дочерний узел корневого узла package. В завис. от того, стоит галочка "Потребление" или нет,
            // это будет узел group или adjacent со своими атрибутами
            Element packageChild;
            if (!checkBoxConsumption.isSelected()) {
                packageChild = xmlDoc.createElement("adjacent");
                packageChild.setAttribute("code-from", textFieldOre.getText());
                packageChild.setAttribute("code-to", comboBoxSubjectOre.getValue());
            } else {
                packageChild = xmlDoc.createElement("group");
                packageChild.setAttribute("code", textFieldOre.getText());
            }
            int k = 1;

            // если нужно менять знак значений, то делаем коэф-т k=-1
            if (checkBoxChangeSign.isSelected())
                k = -1;
            int transferTime = 0;
            int transferDay = 0;

            // если был перевод времени (стоит галка), то получаем время перевода и число
            if (checkBoxTransferTime.isSelected()) {
                transferTime = spinnerHour.getValue();
                transferDay = datePicker.getValue().getDayOfMonth();
            }
            int year = spinnerYear.getValue();
            int month = comboBoxMonth.getSelectionModel().getSelectedIndex();
            int day = 1;
            int hour = 0;
            int minute = 0;

            // инициализируем календарь значениями выбранного года, месяца, 1 числом этого месяца и временем 00:00
            Calendar calendar = new GregorianCalendar(year, month, day, hour, minute);
            formatter = new SimpleDateFormat("yyyyMMddHHmm");
            Sheet sheet = workbook.getSheetAt(0);
            int cellNum = sheet.getRow(0).getLastCellNum();

            String dl = "";
            if (checkBoxTransferTime.isSelected() && radioBtnBack.isSelected())
                dl = "DL";
            if (checkBoxTransferTime.isSelected() && radioBtnForward.isSelected())
                dl = "";

            int rowNum = 23; // количество строк в файле excel должно быть 24 (25 в случае перевода на час назад)
            // проходим по всем строкам каждого столбца (i - столбец, j - строка)
            for (int i = 0; i < cellNum; i++) {
                for (int j = 0; j <= rowNum; j++) {
                    Row row = sheet.getRow(j);
                    int power = (int) row.getCell(i).getNumericCellValue();
                    // если перевод на час вперед, переводим календарь на час вперед и досрочно завершаем j-й шаг
                    // в i сутках будет 23 часа

                    if (checkBoxTransferTime.isSelected() && i == transferDay - 1 && j == transferTime) {
                        if (radioBtnForward.isSelected()) {
                            calendar.add(Calendar.HOUR, 1);
                            dl = "DL"; // все последующие значения интервалов будут оканчиваться на DL
                            continue;
                        }
                    }

                    // в обычном режиме без перевода создаем узлы flow с атрибутами
                    Element flowNode = xmlDoc.createElement("flow");
                    String begin = formatter.format(calendar.getTime()) + "GMT" + comboBoxGMT.getValue() +
                            textFieldTimeZone.getText();
                    calendar.add(Calendar.HOUR, 1);
                    String end = formatter.format(calendar.getTime()) + "GMT" + comboBoxGMT.getValue() +
                            textFieldTimeZone.getText();
                    flowNode.setAttribute("power", Integer.toString(power * k));
                    flowNode.setAttribute("begin", begin + dl);
                    flowNode.setAttribute("end", end + dl);
                    packageChild.appendChild(flowNode);

                    // если перевод на час назад, то создаем с текущими данными календаря еще 1 узел flow,
                    // только значение power берем из 25 строки текущего стобца i
                    if (checkBoxTransferTime.isSelected() && i == transferDay - 1 && j == transferTime - 1) {
                        if (radioBtnBack.isSelected()) {
                            dl = ""; // этот и все последующие узлы будут оканчиваться без DL
                            flowNode = xmlDoc.createElement("flow");
                            Row additionalRow = sheet.getRow(24);
                            if (additionalRow != null)
                                power = (int) sheet.getRow(24).getCell(i).getNumericCellValue();
                            else
                                power = 0;
                            flowNode.setAttribute("power", Integer.toString(power * k));
                            flowNode.setAttribute("begin", begin);
                            flowNode.setAttribute("end", end);
                            packageChild.appendChild(flowNode);
                        }
                    }
                }
            }
            // закрываем книгу
            workbook.close();
            // добавляем в корень узел package со всеми данными
            root.appendChild(packageChild);
            NodeList flowNodes = root.getElementsByTagName("flow");
            // если нужно сравнить с др. файлом xml, получаем список узлов flow 1-го (созданного) файла xml и
            // список узлов flow 2-го файла xml. Данные сравниваем попарно
            if (checkBoxCompare.isSelected()) {
                File file = new File(textFieldXml.getText());
                URL otherXmlFile = file.toURI().toURL();
                Document otherXmlDoc = XmlUtil.getXmlDoc(otherXmlFile);
                Element root2 = otherXmlDoc.getDocumentElement();
                NodeList flowNodes2 = root2.getElementsByTagName("flow");
                // сюда заносим врем. интервалы и различающиеся значения power
                List<String> unCorrectData = new ArrayList<>();
                for (int i = 0; i < flowNodes.getLength(); i++) {
                    String begin1 = flowNodes.item(i).getAttributes().getNamedItem("begin").getNodeValue();
                    String end1 = flowNodes.item(i).getAttributes().getNamedItem("end").getNodeValue();
                    String power = flowNodes.item(i).getAttributes().getNamedItem("power").getNodeValue();
                    String power2 = flowNodes2.item(i).getAttributes().getNamedItem("power").getNodeValue();
                    if (!power.equals(power2)) {
                        unCorrectData.add("Интервал: " + begin1 + " - " + end1 + ". Значения: " + power + " и " + power2);
                    }
                }
                // есть есть различия, выводим их в окне
                if (unCorrectData.size() > 0) {
                    StringBuilder text = new StringBuilder();
                    for (String line : unCorrectData) {
                        text.append(line).append(System.lineSeparator());
                    }
                    textWindow.showModalWindow("Внимание", "Данные в файлах не совпадают!", text.toString(),
                            Alert.AlertType.WARNING);
                } else {
                    messageWindow.showModalWindow("Внимание", "Данные в файлах совпадают!",
                            Alert.AlertType.INFORMATION);
                }
            }
            // под этим именем будем сохранять файл
            String outFileName = "d51070" + sender + createdTime + ".xml";
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(outFileName);

            FileChooser.ExtensionFilter extFilter =
                    new FileChooser.ExtensionFilter("XML файлы", "*.xml");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showSaveDialog(new Stage());
            if (file != null) {
                XmlUtil.saveXMLDoc(xmlDoc, file.getAbsolutePath(), "windows-1251", true);
            }
        } catch (Exception e) {
            messageWindow.showModalWindow("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void intCellStyles(Workbook workbook) {
        // создаем стиль для ячеек с некоммерч. инф.: жирный красный курсив
        redTextStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);
        redFont.setItalic(true);
        redTextStyle.setFont(redFont);

        // создаем стиль для ячеек в заголовках листов: черный жирный
        boldFontStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font boldBlackFont = workbook.createFont();
        boldBlackFont.setBold(true);
        boldFontStyle.setFont(boldBlackFont);

        // создаем стиль ячейки: верт. и гориз. выравнивание по центру и тонкие границы и жирный шрифт
        boldAndBorderStyle = workbook.createCellStyle();
        boldAndBorderStyle.setBorderBottom(BorderStyle.THIN);
        boldAndBorderStyle.setBorderLeft(BorderStyle.THIN);
        boldAndBorderStyle.setBorderRight(BorderStyle.THIN);
        boldAndBorderStyle.setBorderTop(BorderStyle.THIN);
        boldAndBorderStyle.setAlignment(HorizontalAlignment.CENTER);
        boldAndBorderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // создаем стиль для ячейки, на которую сработал обх. выключатель: жирный шрифт с заливкой
        boldAndFillStyle = workbook.createCellStyle();
        boldAndFillStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        boldAndFillStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        boldAndFillStyle.setFont(boldBlackFont);
    }

    // инициализация контролов и загрузка ресурсов в Tab-ах
    private void initTab80020() {
        // загружаем пиктограммы на кнопки
        Image imageXml = new Image("Resources/xls.png");
        btnMakeExcel.graphicProperty().setValue(new ImageView(imageXml));

        Image imageXls = new Image("Resources/xml.png");
        btnMake80020.graphicProperty().setValue(new ImageView(imageXls));

        Image imageReload = new Image("Resources/reload.png");
        btnReload.graphicProperty().setValue(new ImageView(imageReload));

        // помещаем радио кнопки в одну группу выбора
        ToggleGroup toggleGroup = new ToggleGroup();
        radioButton30Min.setToggleGroup(toggleGroup);
        radioButton60Min.setToggleGroup(toggleGroup);

        // при выборе нового файла будет считываться информация из него (добавляется слушатель)
        filesListView.getSelectionModel().selectedItemProperty().addListener((ov, old_value,
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
                for (MeasuringChannel measuringChannel : measuringPoint.getMeasChannelList()) {

                    // в пункт контекс. меню каждого measuringChannel-а добавляем слушателя установки некомм. инф.,
                    // в котором меняется тип информации на некомм. и цвет кода текущ. measuringPoint-а
                    measuringChannel.getUnCommMenuItem().setOnAction(event -> {
                        List<Period> periodList = measuringChannel.getPeriodList();
                        measuringChannel.setCommercialInfo(false);
                        measuringChannel.setFont(Font.font("System", FontPosture.ITALIC, -1));
                        labelMeasPointCode.setTextFill(Color.RED);
                        if (periodList != null)
                            periodList.forEach(period -> period.setStatus("1"));
                    });
                    // в пункт контекс. меню каждого measuringChannel-а добавляем показа данных,
                    measuringChannel.getShowDataItem().setOnAction(event -> {
                        dataWinControl.labelMeasPoint.setText(measuringPoint.getName());
                        dataWinControl.labelMeasChannel.setText(measuringChannel.getAliasName());
                        // получаем список узлов value
                        List<Period> periodList = measuringChannel.getPeriodList();
                        //ObservableList<MeasuringData> measDataList = FXCollections.observableArrayList();
                        ObservableList<Period> measDataList = FXCollections.observableArrayList();
                        measDataList.addAll(periodList);
                        dataWinControl.dataTableView.setItems(measDataList);
                        dataWinControl.dataTableView.scrollTo(0);
                        dataWinControl.dataTableView.refresh();
                        dataStage.show();
                    });

                    // добавляем слушателя на событие выбора/снятия галочки с канала
                    // если галочки сняты со всех каналов, то снимаем галочку с текущ. measuringPoint-а
                    measuringChannel.setOnAction(event -> {

                        int countDeselected = 0;
                        for (MeasuringChannel measChannel : measuringPoint.getMeasChannelList()) {
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

        textViewAIIS.textProperty().addListener(event -> {
            if (textViewAIIS.getText() != null && !textViewAIIS.getText().equals(""))
                btnDelAIIS.setDisable(false);
            else
                btnDelAIIS.setDisable(true);
        });
    }

    // при инициализации гл. окна программы создаем окно с настройками
    private void initSettingsWindow() throws IOException {
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
    }

    // при инициализации гл. окна программы создаем окно c данными по каналам ТИ
    private void initDataWindow() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
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
    }

    // при инициализации гл. окна программы создаем окно "О программе"
    private void initAboutWindow() {
        aboutWindow.setTitle("О программе");
        aboutWindow.setHeaderText("АСКУЭ 1.0");
        aboutWindow.setContentText("Для использования только в ПАО \"АЭСК\"" + System.lineSeparator() +
                "Разработчик Ищенко С.А. " + System.lineSeparator() + "e-mail: astraboomer@hotmail.com");
    }

    @FXML
    private void initialize() {
        try {
            initSettingsWindow();
            initDataWindow();
            initAboutWindow();
            initTab80020();
            initTab51070();
            initTabAdditional();

        } catch (IOException e) {
            // выводим сообщение об шибке в случае неудачи
            messageWindow.showModalWindow("Ошибка", e.getMessage() + ". Программа будет закрыта.",
                    Alert.AlertType.ERROR);
            Platform.exit();
            System.exit(0);
        }
    }

    // заполнение списка measuringpoint-ов
    private void fillMeasPointListView(XML80020 xmlClass) {
        ObservableList<MeasuringPoint> measPointObList = FXCollections.observableArrayList();
        // помещаем все measuringPoint-ы из всех area в measPointObList
        for (Area area : xmlClass.getAreaList())
            measPointObList.addAll(area.getMeasPointList());
        measPointListView.setItems(measPointObList);
        // добавляем слушателя свойству выбора каждого элемента списка
        measPointObList.forEach(measPoint -> measPoint.selectedProperty().addListener((observable, wasSelected,
                                                                                       isSelected) -> {
            if (isSelected) {
                int countMeasPoints = Integer.parseInt(labelSelectedMeasPoints.getText());
                labelSelectedMeasPoints.setText(Integer.toString(countMeasPoints + 1));
                for (MeasuringChannel measChannel : measPoint.getMeasChannelList()) {
                    measChannel.setSelected(true);
                    measChannel.setDisable(false);
                }
            } else {
                for (MeasuringChannel measChannel : measPoint.getMeasChannelList()) {
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
            btnMakeExcel.setDisable(false);
            comboBoxAreaName.setDisable(false);
            btnReload.setDisable(false);
            textViewAIIS.setDisable(false);
            btnSaveAIIS.setDisable(false);
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
        for (MeasuringChannel measuringChannel : measuringPoint.getMeasChannelList()) {
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

    public void loadSubjectSettings() {
        textViewAIIS.setText("");
        this.subjects = new HashMap<>();
        Document xmlDoc = settingsWinControl.getSettingsXmlDoc();
        NodeList subjectNodeList = xmlDoc.getDocumentElement().getElementsByTagName("subject");
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
        fileChooser.setInitialDirectory(lastOpenDir); /// удалить потом!
        fileChooser.setTitle("Выберите файлы XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файлы XML", "*.xml"));
        // вводим лок.  переменную localFileList, которая инициализируется при каждом нажатии на кнопку
        // "Выбрать файлы XML".
        List<File> localFileList = fileChooser.showOpenMultipleDialog(new Stage());
        ObservableList<String> fileNames = FXCollections.observableArrayList();

        // если файлы выбраны (не нажата кнопка "отмена" диалога выбора файлов), т.е.
        // localFileList != null, то ей же присваиваем список корректных xml-файлов
        // если в localFileList не окажется файлов, то дальнейших действий не будет и
        // список файлов fileList остается с предыдущего удачного выбора файлов,
        // иначе fileList получает значение localFileList с коррект. списком файлов
        if (localFileList != null) {
            localFileList = ServiceUtil.validateXMLFiles(localFileList); // оставляем в списке только коррект. xml-файлы
            if (localFileList.size() > 0) { // если размер списка корректных файлов больше 0
                lastOpenDir = localFileList.get(0).getParentFile();
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
    private void setControlsDisabled(boolean enabled) {
        comboBoxAreaName.setDisable(enabled);
        btnReload.setDisable(enabled);
        btnSaveAIIS.setDisable(enabled);
        textViewAIIS.setDisable(enabled);
        btnMake80020.setDisable(enabled);
    }

    // показ данных из файла file (чтение из xml-файла, заполнение списка measuringpoint-ов и
    // measuringchannel-ов)
    private void displayAllXMLData(File file) {
        // в любой момент времени после загрузки xml-файла доступен только 1 объект класса 80020 или 80025
        // поэтому при вызове этого метода "обнуляем" оба, т.к. до этого этим объектом мог быть как
        // 80020, так и 80025. Далее в завис. от типа, с которого начинается имя файла
        // создаем объект нужного класса

        if (file.getName().startsWith("80020") || file.getName().startsWith("80040")) {
            currentXml = new XML80020(file);
            fillMeasPointListView(currentXml);
            setControlsDisabled(false);
        }
        else {
            currentXml = new XML80025(file);
            fillMeasPointListView(currentXml);
            setControlsDisabled(true);
        }
    }

    // выбор всех measuringpoint-ов (вызывается при нажатии на кнопку [ v ])
    @FXML
    private void selectAllMeasPoints(ActionEvent actionEvent) {
        for (Object measPoint : measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint) measPoint;
            measuringPoint.setSelected(true);
        }
    }

    // снятие выбора со всех measuringpoint-ов (вызывается при нажатии на кнопку [  ])
    @FXML
    private void unSelectAllMeasPoints(ActionEvent actionEvent) {
        for (Object measPoint : measPointListView.getItems()) {
            MeasuringPoint measuringPoint = (MeasuringPoint) measPoint;
            measuringPoint.setSelected(false);
        }
    }

    // создание файла excel (выгрузка данных)
    @FXML
    private void makeExcel(ActionEvent actionEvent) throws InterruptedException {
        // запоминаем тек. время
        Long startTime = new Date().getTime();
        // списки названий и кодов для выбранных ТИ
        List<String> mpNames = new ArrayList<>();
        List<String> mpCodes = new ArrayList<>();
        // создаем новую книгу Excel
        XSSFWorkbook wb = new XSSFWorkbook();
        SXSSFWorkbook workbook = new SXSSFWorkbook(wb, 100);

        // создаем в книге 4 листа и задаем им цвет.
        workbook.createSheet(ACTIVE_INPUT).setTabColor(new XSSFColor(colors[0]));
        workbook.createSheet(ACTIVE_OUTPUT).setTabColor(new XSSFColor(colors[1]));
        workbook.createSheet(REACTIVE_INPUT).setTabColor(new XSSFColor(colors[2]));
        workbook.createSheet(REACTIVE_OUTPUT).setTabColor(new XSSFColor(colors[3]));

        // создаем различные стили для ячеек
        intCellStyles(workbook);

        // заносим в соотв. списки названия и коды отмеченных ТИ
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
        createSheetsHeader(workbook, mpNames, mpCodes);
        btnMake80020.setDisable(true);
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception { // запускается при старте thread-а

                if (!checkBoxBatch.isSelected()) {  // если НЕ пакетная обработка, то выгружаем один выбранный файл
                                                    // (он может быть и первым в списке файлов)
                    xmlDataToXls(workbook, 0);
                    updateProgress(1, 1); // задаем значение прогрессбара, как 1 из 1
                }
                else { // если пакетная обработка, то перебираем все файлы

                    XML80020 xmlFirst = currentXml;
                    for (int i = 0; i < fileList.size(); i++) {
                        XML80020 prevXml = currentXml;
                        if (fileList.get(i).getName().contains("80020") || fileList.get(i).getName().contains("80040")) {
                            currentXml = new XML80020(fileList.get(i));
                        } else {
                            currentXml = new XML80025(fileList.get(i));
                        }
                        copySelectedProperty(prevXml, currentXml);
                        xmlDataToXls(workbook, i);  // передаем workbook и индекс файла в списке файлов, индекс
                                                    // нужен  для формирования номеров строк в excel
                        updateProgress(i + 1, fileList.size()); // обновляем значение прогрессбара
                    }
                    // текущему xml-файлу возращаем данные первого файла в списке (он является "выделенным" в списке)
                    currentXml = xmlFirst;
                }
                // выводим суммы значений по столбцам
                sumArrayToSheet(workbook, sumArray);
                // выводим коды ТИ, на которые сработал обх. выключатель
                mpCodesToSheet(workbook);
                // делаем акт. первый лист книги
                workbook.setActiveSheet(0);
                // метод call ничего не будет возвращать
                return null;
            }
            @Override protected void succeeded() { // срабатывает при успешном завершении task-а
                super.succeeded();
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
                if (!(currentXml instanceof XML80025)) btnMake80020.setDisable(false);
                if (file != null) {
                    try {
                        if (!(file.getName().endsWith(".xlsx"))) {
                            String newFileName = file.getAbsolutePath() + ".xlsx";
                            file = new File(newFileName);
                        }
                        FileOutputStream fos = new FileOutputStream(file);
                        workbook.write(fos);
                        workbook.close();
                        wb.close();
                        fos.close();
                    } catch (IOException e) {
                        messageWindow.showModalWindow("Ошибка", "Не удается сохранить файл " + file.getName() +
                                ". Возможно он открыт в другой программе.", Alert.AlertType.ERROR);
                    }
                }
            }
        };

        // связываем свойство прогресса прогрессбара со свойством прогресса task-а
        progressBar.progressProperty().bind(task.progressProperty());
        // запускаем выгрузку в excel (task) в отдельном thread-е
        Thread thread = new Thread(task);
        thread.start();
    }

    // метод выводит коды ТИ, на которые сработал обх. выключатель и ставит гиперссылки на них
    private void mpCodesToSheet(SXSSFWorkbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            int rowNum = sheet.getLastRowNum() + 2;
            for (int j = 0; j < extCodes[i].size(); j++) {
                SXSSFRow row = sheet.createRow(rowNum + j);
                SXSSFCell codeCell = row.createCell(2);
                codeCell.setCellValue(extCodes[i].get(j).getStringCellValue());

                Hyperlink href = workbook.getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
                int columnIndex = extCodes[i].get(j).getColumnIndex();
                CellReference cellRef = new CellReference(1, columnIndex);
                href.setAddress(cellRef.formatAsString());
                codeCell.setHyperlink(href);
            }
        }
    }

    // метод устанавливает свойство selected ТИ и каналов объекта dest в соотв. с теми же значениями
    // что и у объета source
    private static void copySelectedProperty(XML80020 source, XML80020 dest) {
        for (int i = 0; i < source.getAreaList().size(); i++) {
            for (int j = 0; j < source.getAreaList().get(i).getMeasPointList().size(); j++) {
                boolean selected = source.getAreaList().get(i).getMeasPointList().get(j).isSelected();
                dest.getAreaList().get(i).getMeasPointList().get(j).setSelected(selected);
            }
        }
    }

    // метод преобразует строку str в число и возвращает его, если не получается, то возвращает 0
    private int strToInt (String str) {
        int val;
        try {
            val = Integer.parseInt(str);
        }
        catch (NumberFormatException e)
        {
            val = 0;
        }
        return val;
    }

    // метод переносит значения из currentXml в книгу workbook используя порядковый номер файла
    private void xmlDataToXls(SXSSFWorkbook workbook, int fileNum) {
        int columnNum = 0;
        int pointNum = 0;
        for (int z = 0; z < currentXml.getAreaList().size(); z++) {
            for (int i = 0; i < currentXml.getAreaList().get(z).getMeasPointList().size(); i++) {
                if (currentXml.getAreaList().get(z).getMeasPointList().get(i).isSelected()) {
                    List<MeasuringChannel> measChannelList = currentXml.getAreaList().get(z).getMeasPointList().get(i).
                            getMeasChannelList();
                    for (int j = 0; j < measChannelList.size(); j++) {
                        String aliasChannelName = measChannelList.get(j).getAliasName();
                        Period[] periods = measChannelList.get(j).getPeriodList().toArray(new Period[measChannelList.
                                get(j).getPeriodList().size()]);

                        mcValuesToSheet(workbook, fileNum, currentXml.getDateTime().getDay(), aliasChannelName,
                                columnNum, periods);

                        switch (aliasChannelName) {
                            case ACTIVE_INPUT:
                                sumArray[0][pointNum] += Arrays.stream(periods).mapToLong(p ->
                                        strToInt(p.getValue())).sum();
                                break;
                            case ACTIVE_OUTPUT:
                                sumArray[1][pointNum] += Arrays.stream(periods).mapToLong(p ->
                                        strToInt(p.getValue())).sum();
                                break;
                            case REACTIVE_INPUT:
                                sumArray[2][pointNum] += Arrays.stream(periods).mapToLong(p ->
                                        strToInt(p.getValue())).sum();
                                break;
                            case REACTIVE_OUTPUT:
                                sumArray[3][pointNum] += Arrays.stream(periods).mapToLong(p ->
                                        strToInt(p.getValue())).sum();
                                break;
                        }
                    }
                    columnNum++;
                    pointNum++;
                }
            }
        }
    }

    // метод выводит в книге на каждом листе суммы значений по столбцам
    private void sumArrayToSheet(SXSSFWorkbook workbook, long sumArray[][]) {
        for (int i = 0; i < sumArray.length; i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            int rowSum = sheet.getLastRowNum() + 1;
            SXSSFRow row = sheet.createRow(rowSum);
            for (int j = 0; j < sumArray[i].length; j++) {
                SXSSFCell cell = row.createCell(2 + j);
                cell.setCellValue(sumArray[i][j]);
                cell.setCellStyle(boldFontStyle);
            }

        }
    }

    // принамает книгу excel, поряд. номер файла, дату, алиасное имя канала, поряд. номер ТИ из списка, массив
    // значений в канале (48 получасовок) и формирует даныые в книге excel
    private void mcValuesToSheet(SXSSFWorkbook workbook, int fileNum, String day, String aliasChannelName,
                                 int columnNum, Period[] periods) {
        // по алиасному имени узнаем, в какой лист выводить данные
        SXSSFSheet sheet = workbook.getSheet(aliasChannelName);
        String period;

        int z; // делитель временных интервалов: 1 (30 мин) или 2 (для 60 мин)
        if (radioButton30Min.isSelected())
            z = 1;
        else
            z = 2;

        for (int i = 0; i < periods.length; i++) {
            SXSSFRow row = sheet.getRow(2 + (i / z) + (fileNum * (periods.length / z)));
            if (row == null)
                row = sheet.createRow(2 + (i / z) + (fileNum * (periods.length / z)));
            // в первый столбец каждой строки выводится дата
            row.createCell(0).setCellValue(Integer.parseInt(day));
            // если нужно выводить врем. интервал
            if (checkBoxShowIntervals.isSelected()) {
                if (row.getCell(1) == null) {
                    String startPeriod = "[" + periods[i].getStart().substring(0, 2) + "." +
                            periods[i].getStart().substring(2, 4);
                    String endPeriod;

                    if (z == 2) {
                        endPeriod = periods[i + 1].getEnd().substring(0, 2) + "." +
                                periods[i + 1].getEnd().substring(2, 4) + "]";
                    } else
                        endPeriod = periods[i].getEnd().substring(0, 2) + "." +
                                periods[i].getEnd().substring(2, 4) + "]";

                    period = startPeriod + " - " + endPeriod;
                    row.createCell(1).setCellValue((Integer.toString((i / z) + 1)) + " " + period);
                }
            }
            // иначе
            else {
                row.createCell(1).setCellValue((i / z) + 1);
            }
            int value;
            try {
                value = Integer.parseInt(periods[i].getValue());
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (row.getCell(2 + columnNum) == null)
                row.createCell(2 + columnNum).setCellValue(value);
            else {
                int prevValue = (int) row.getCell(2 + columnNum).getNumericCellValue();
                row.getCell(2 + columnNum).setCellValue(value + prevValue);
            }

            // если инф. некоммер., то шрифт - красный толстый курсив
            if (periods[i].getStatus() != null && periods[i].getStatus().equals("1"))
               row.getCell(2 + columnNum).setCellStyle(redTextStyle);

            // если есть extendedstatus (сработал обх. выключатель), то помечаем это значение
            if (periods[i].getExtendedStatus() != null && periods[i].getExtendedStatus().equals("1114")) {
                if (row.getCell(2 + columnNum).getCellComment() == null)
                    ExcelUtil.setCellComment(row.getCell(2 + columnNum), periods[i].getParam1());

                for (int j = 2; j < headerCodesRow.getLastCellNum(); j++) {
                    if (headerCodesRow.getCell(j).getStringCellValue().equals(periods[i].getParam1())) {
                        if (!extCodes[workbook.getSheetIndex(sheet)].contains(headerCodesRow.getCell(j)))
                            extCodes[workbook.getSheetIndex(sheet)].add(headerCodesRow.getCell(j));

                        Cell cell = row.getCell(j);
                        if (cell == null) {
                            cell = row.createCell(j);
                        }
                        cell.setCellStyle(boldAndFillStyle);
                        break;
                    }

                }
            }
        }
    }

    // метод выводит "шапку" из имени и кода ТИ на переданном листе
    private static void createSheetsHeader(SXSSFWorkbook workbook, List<String> names, List<String> codes) {
        // создаем стиль для ячеек с именем ТИ
        CellStyle[] fillAndBorderStyle = new CellStyle[colors.length];
        for (int i = 0; i < fillAndBorderStyle.length; i++) {
            fillAndBorderStyle[i] = workbook.createCellStyle();
            fillAndBorderStyle[i].setBorderBottom(BorderStyle.THIN);
            fillAndBorderStyle[i].setBorderLeft(BorderStyle.THIN);
            fillAndBorderStyle[i].setBorderRight(BorderStyle.THIN);
            fillAndBorderStyle[i].setBorderTop(BorderStyle.THIN);
            fillAndBorderStyle[i].setAlignment(HorizontalAlignment.CENTER);
            fillAndBorderStyle[i].setVerticalAlignment(VerticalAlignment.CENTER);
            fillAndBorderStyle[i].setWrapText(true);

            // создаем шрифт и применяем его к стилю
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setFontHeight((short)180);
            //font.setBold(true);
            fillAndBorderStyle[i].setFont(font);
            fillAndBorderStyle[i].setFillForegroundColor(colors[i].getIndex());
            fillAndBorderStyle[i].setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        Row rowName;
        SXSSFRow rowCode = null;
        // со всеми листами книги делаем следующее
        for (int k = 0; k < workbook.getNumberOfSheets(); k++) {
            SXSSFSheet sheet = workbook.getSheetAt(k);
            // создаем строки для имен  и кодов
            rowName = sheet.createRow(0);
            rowCode = sheet.createRow(1);
            // создаем ячейки
            rowName.createCell(0).setCellValue("Дата");
            rowName.createCell(1).setCellValue("Время");
            // устанавливаем стили для ячеек Дата и Время
            sheet.getRow(0).getCell(0).setCellStyle(boldAndBorderStyle);
            sheet.getRow(0).getCell(1).setCellStyle(boldAndBorderStyle);
            // создаем ячейки под "Дата" и "Время" и устанавливаем стили для них
            rowCode.createCell(0);
            rowCode.createCell(1);
            rowCode.getCell(0).setCellStyle(boldAndBorderStyle);
            rowCode.getCell(1).setCellStyle(boldAndBorderStyle);
            // объединяем ячейки "Дата" и "Время" с теми, что под ними
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));

            // создаем ячейки с именами и кодами ТИ и применяем стили к ним
            for (int i = 0; i < names.size(); i++) {
                sheet.setColumnWidth(2 + i, 4200);
                rowName.createCell(2 + i).setCellValue(names.get(i));
                rowName.setHeightInPoints(40);

                rowCode.createCell(2 + i).setCellValue(codes.get(i));
                rowName.getCell(2 + i).setCellStyle(fillAndBorderStyle[k]);
                rowCode.getCell(2 + i).setCellStyle(boldAndBorderStyle);
            }
        }
        headerCodesRow = rowCode;
    }

    // создание xml- файла (вызывается при нажатии на кнопку "Создать макет XML")
    @FXML
    private void makeXML(ActionEvent actionEvent) {
        // получение значений элементов окна настроек через переменную settingsWinControl
        String senderName = settingsWinControl.textFieldName.getText();
        String senderINN = settingsWinControl.textFieldINN.getText();
        String areaName;
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

        String outDirName;

        if (autoSaveDir != null) { // если обрабатываем только выделенный файл
            outDirName = autoSaveDir;
        } else { // если передали null-е значение, то сохраняем в ту же папку
            // в подпапку с именем класса макета (80020 или 80040)
            // если она не сущ., то создаем ее
            outDirName = currentXml.getFile().getParent() + slash + currentXml.getMessage().getMessageClass();
            if (!Files.exists(Paths.get(outDirName)))
                new File(outDirName).mkdir();
        }


        Task<Void> task = new Task<Void>() {
            String messNumber;
            int num = Integer.parseInt(settingsWinControl.textFieldNumber.getText());
            String outFileName;
            @Override
            protected Void call() throws Exception {

                if (!checkBoxBatch.isSelected()) { // если не пакетная обработка
                    if (num == Integer.MAX_VALUE) // если достигнуто макс. знач. Integer, то начинаем с 0
                        num = 0;
                    messNumber = Integer.toString(num + 1); // увеличиваем на 1 номер message-а
                    settingsWinControl.textFieldNumber.setText(messNumber);
                    // под этим именем сохраняем файл
                    String fileName = currentXml.getMessage().getMessageClass() + "_" +
                            senderINN + "_" +
                            currentXml.getDateTime().getDay() + "_" +
                            messNumber + "_" +
                            senderAIIS + ".xml";

                    outFileName = outDirName + slash + fileName;
                    try {
                        currentXml.saveDataToXML(senderName, senderINN, areaName, areaINN, messVersion, messNumber,
                                newDLSavingTime, outFileName);
                        // обновляем значение индикатора
                        updateProgress(1, 1);
                    } catch (TransformerException e) {
                        messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + outFileName +
                                " завершена неудачно!", Alert.AlertType.ERROR);
                        //return;
                    }

                } else // если пакетная обработка
                {
                    // запоминаем данные первого xml-файла в списке
                    XML80020 xmlFirst = currentXml;
                    int n = 0;
                    for (File file : fileList) {
                        n++;
                        if (num == Integer.MAX_VALUE) // если достигнуто макс. знач. Integer, то начинаем с 0
                            num = 0;
                        num++; // увеличиваем на 1 номер message-а
                        messNumber = Integer.toString(num);
                        XML80020 xmlTemp = currentXml;
                        currentXml = new XML80020(file);
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
                                senderINN + "_" +
                                currentXml.getDateTime().getDay() + "_" +
                                messNumber + "_" +
                                senderAIIS + ".xml";

                        outFileName = outDirName + slash + fileName;
                        try {
                            currentXml.saveDataToXML(senderName, senderINN, areaName, areaINN, messVersion, messNumber,
                                    newDLSavingTime, outFileName);
                        } catch (TransformerException e) {
                            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + outFileName +
                                    " завершена неудачно!", Alert.AlertType.ERROR);
                            //return;
                        }
                        // обновляем значение индикатора
                        updateProgress(n, fileList.size());
                    }
                    // текущему xml-файлу возращаем данные первого файла в списке (он является "выделенным" в списке)
                    currentXml = xmlFirst;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                messageWindow.showModalWindow("Выполнено", "Данные сохранены в папке " + outDirName,
                        Alert.AlertType.INFORMATION);
                settingsWinControl.textFieldNumber.setText(messNumber);
                settingsWinControl.saveNumber(messNumber); // сразу сохраняем новый номер в файл настроек
            }
        };

        // связываем свойство прогресса прогрессбара со свойством прогресса task-а
        progressBar.progressProperty().bind(task.progressProperty());
        // запускаем формирование макетов 80020/80040 (task) в отдельном thread-е
        Thread thread = new Thread(task);
        thread.start();
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
                ServiceUtil.messageWindow.showModalWindow("Внимание", "Имя контрагента и " +
                        "его код не должны быть пустыми!", Alert.AlertType.INFORMATION);
                return;
            }
            if (subjectNodeList.item(i).getAttributes().getNamedItem("code").getNodeValue().equals(aiis)) {
                ServiceUtil.messageWindow.showModalWindow("Внимание", "Контрагент с таким кодом АИИС уже " +
                        "существует. Сохраните его под другим кодом!", Alert.AlertType.INFORMATION);
                return;
            }
            if (subjectNodeList.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                ServiceUtil.messageWindow.showModalWindow("Внимание", "Контрагент с таким именем уже " +
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
            } else {
                // узнаем количество "выбранных" каналов у measuringPoint-а;
                long countSelectedChannel = measuringPoint.getMeasChannelList().stream().
                        filter(CheckBox::isSelected).count();
                if (countSelectedChannel != measuringPoint.getMeasChannelList().size()) {
                    for (MeasuringChannel measuringChannel : measuringPoint.getMeasChannelList()) {
                        if (!measuringChannel.isSelected()) {
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
        } catch (TransformerException e) {
            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " +
                    settingsWinControl.getFileName() + " завершена неудачно!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void delAIIS(ActionEvent actionEvent) {
        Optional<ButtonType> result = messageWindow.showModalWindow("Удаление настроек выбора", "Вы " +
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
        } catch (TransformerException e) {
            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " +
                    settingsWinControl.getFileName() + " завершена неудачно!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void batchProcessing(ActionEvent actionEvent) {
        if (checkBoxBatch.isSelected()) {
            filesListView.getSelectionModel().selectFirst();
            filesListView.setDisable(true);
            progressBar.setVisible(true);
            // устанавливаем значение прогрессбара в 0
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        } else {
            filesListView.setDisable(false);
            progressBar.setVisible(false);
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
        for (Object object : measPointListView.getItems()) {
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
                        for (Object object : measPointListView.getItems()) {
                            MeasuringPoint measuringpoint = (MeasuringPoint) object;

                            if (measuringpoint.getCode().equals(measPointCode)) {
                                int measChannelNum = measPointsNodeList.item(j).getChildNodes().getLength();
                                boolean hasChannels = measChannelNum > 0;
                                if (!hasChannels) {
                                    measuringpoint.setSelected(false);
                                } else {
                                    for (int k = 0; k < measChannelNum; k++) {
                                        for (MeasuringChannel measuringChannel : measuringpoint.getMeasChannelList()) {
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
