package Controllers;

import Classes.*;
import Classes.XmlTag.Area;
import Classes.XmlTag.MeasuringChannel;
import Classes.XmlTag.MeasuringPoint;
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
import static Classes.XML80020.*;
import static Classes.XmlClass.messageWindow;
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
    private final static int[] colorNums = new int[]{11, 12, 20, 14, 62, 23, 25, 44, 28, 29, 45, 46, 52, 60, 49, 40};
    // массив списков ячеек Excel из строки с кодами ТИ в шапке, на которые срабатывает обходной выключатель
    private List<XSSFCell>[] extCodes;
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
                else {                                                // добавляем его в формируемую строку
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

    protected static void unZipJarFile (File file, String fileName) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos;
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            String fileNameInZip = zipEntry.getName();
            if (fileNameInZip.equals(fileName)) {
                byte[] buffer = new byte[1000];
                int count;
                fos = new FileOutputStream(fileNameInZip);
                while ((count = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
    }

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


    // инициализация контролов и загрузка ресурсов в Tab-ах
    private void initTab80020() {

        // загружаем пиктограммы на кнопки
        Image imageXml = new Image("Resources/xls.png");
        btnMakeXLS.graphicProperty().setValue(new ImageView(imageXml));

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
                            } else {
                                typeInfo = "0";
                            }
                            String timeInterval = start.substring(0, 2) + ":" + start.substring(2, 4) +
                                    " - " + end.substring(0, 2) + ":" + end.substring(2, 4);
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

    // метод возвращает список узлов value переданного measuringPoint-а и узла measuringChannel в нем
    private NodeList getValuesOfChannelNode(MeasuringPoint measuringPoint, MeasuringChannel measuringChannel) {
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
            localFileList = XmlClass.validateXMLFiles(localFileList); // оставляем в списке только коррект. xml-файлы
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
    private void makeXLS(ActionEvent actionEvent) throws InterruptedException {
        // запоминаем тек. время
        Long startTime = new Date().getTime();
        // списки названий и кодов для выбранных ТИ
        List<String> mpNames = new ArrayList<>();
        List<String> mpCodes = new ArrayList<>();
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
                    XML80020 prevXml = null;
                    for (int i = 0; i < fileList.size(); i++) {
                        prevXml = currentXml;
                        if (fileList.get(i).getName().contains("80020") || fileList.get(i).getName().contains("80040")) {
                            currentXml = new XML80020(fileList.get(i));
                        } else {
                            currentXml = new XML80025(fileList.get(i));
                        }
                        currentXml.loadDataFromXML();
                        copySelectedProperty(prevXml, currentXml);
                        xmlDataToXls(workbook, i);  // передаем workbook и индекс файла в списке файлов, индекс
                                                    // нужен  для формирования номеров строк в excel
                        updateProgress(i + 1, fileList.size()); // обновляем значение прогрессбара
                    }

                    // после пакетной обработки всех файлов делаем "текущим" первый в списке
                    if (fileList.get(0).getName().contains("80020") || fileList.get(0).getName().contains("80040")) {
                        currentXml = new XML80020(fileList.get(0));
                    } else {
                        currentXml = new XML80025(fileList.get(0));
                    }
                    currentXml.loadDataFromXML();
                    copySelectedProperty(prevXml, currentXml);
                }

                // выводим суммы значений по столбцам и коды ТИ на которые произошло срабатывание обх. переключателя
                sumArrayToSheet(workbook, sumArray);
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

    private static void copySelectedProperty(XML80020 source, XML80020 dest) {
        for (int i = 0; i < source.getAreaList().size(); i++) {
            for (int j = 0; j < source.getAreaList().get(i).getMeasPointList().size(); j++) {
                boolean selected = source.getAreaList().get(i).getMeasPointList().get(j).isSelected();
                dest.getAreaList().get(i).getMeasPointList().get(j).setSelected(selected);
            }
        }
    }

    private void xmlDataToXls(XSSFWorkbook workbook, int fileNum) {
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
                            } catch (NumberFormatException e) {
                                periods[k].setValue(0);
                            }
                        }
                        mcValuesToSheet(workbook, fileNum, currentXml.getDateTime().getDay(), aliasChannelName,
                                columnNum, periods);

                        switch (aliasChannelName) {
                            case activeInput:
                                sumArray[0][pointNum] += Arrays.stream(periods).
                                        mapToLong(Period30Min::getValue).sum();
                                break;
                            case activeOutput:
                                sumArray[1][pointNum] += Arrays.stream(periods).
                                        mapToLong(Period30Min::getValue).sum();
                                break;
                            case reactiveInput:
                                sumArray[2][pointNum] += Arrays.stream(periods).
                                        mapToLong(Period30Min::getValue).sum();
                                break;
                            case reactiveOutput:
                                sumArray[3][pointNum] += Arrays.stream(periods).
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
    private void sumArrayToSheet(Workbook workbook, long sumArray[][]) {
        for (int i = 0; i < sumArray.length; i++) {
            Sheet sheet = workbook.getSheetAt(i);
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

            if (row.getCell(2 + columnNum) == null)
                row.createCell(2 + columnNum).setCellValue(periods[i].getValue());
            else {
                int prevValue = (int) row.getCell(2 + columnNum).getNumericCellValue();
                row.getCell(2 + columnNum).setCellValue(periods[i].getValue() + prevValue);
            }

            // если инф. некоммер., то шрифт - красный толстый курсив
            if (periods[i].getStatus().equals("1"))
                ExcelUtil.setCellFont((XSSFCell) row.getCell(2 + columnNum), IndexedColors.RED, true, true,
                        false, false);

            // если есть extendedstatus (сработал обх. выключатель), то помечаем это значение
            if (periods[i].getExtendedstatus() != null && periods[i].getExtendedstatus().equals("1114")) {
                if (row.getCell(2 + columnNum).getCellComment() == null)
                    ExcelUtil.setCellComment(row.getCell(2 + columnNum), periods[i].getParam1());
                XSSFRow rowCode = sheet.getRow(1);

                for (int j = 2; j < rowCode.getLastCellNum(); j++) {
                    if (rowCode.getCell(j).getStringCellValue().equals(periods[i].getParam1())) {
                        if (!extCodes[workbook.getSheetIndex(sheet)].contains(rowCode.getCell(j)))
                            extCodes[workbook.getSheetIndex(sheet)].add((XSSFCell)rowCode.getCell(j));
                        int rowCodeColor = rowCode.getCell(j).getCellStyle().getFont().getColor();
                        ExcelUtil.setCellFont((XSSFCell) row.getCell(2 + columnNum), IndexedColors.fromInt(rowCodeColor),
                                false, false, false, false);
                        Cell cell = row.getCell(j);
                        if (cell == null) {
                            cell = row.createCell(j);
                        }
                        ExcelUtil.setCellColorAndFontColor((XSSFCell) cell, IndexedColors.fromInt(rowCodeColor),
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
        //fillAndBorderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        //fillAndBorderStyle.setFillPattern(FillPatternType.DIAMONDS);
        // создаем ячейки с именами и кодами ТИ и применяем стили к ним
        for (int i = 0; i < names.size(); i++) {
            sheet.setColumnWidth(2 + i, 4200);
            rowName.createCell(2 + i).setCellValue(names.get(i));
            rowName.setHeightInPoints(40);

            rowCode.createCell(2 + i).setCellValue(codes.get(i));
            rowName.getCell(2 + i).setCellStyle(fillAndBorderStyle);
            rowCode.getCell(2 + i).setCellStyle(borderStyle);
            ExcelUtil.setCellFont((XSSFCell) rowCode.getCell(2 + i),
                    IndexedColors.fromInt(colorNums[i % colorNums.length]),
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
