package Controllers;

import Classes.XmlUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Files;

import static Classes.Main.slash;
import static Classes.ServiceUtil.messageWindow;
import static Controllers.MainWindowController.*;

/**
 * Created by Сергей on 29.05.2017.
 */
public class SettingsWindowController {

    public TextField textFieldINN;
    public TextField textFieldAIIS;
    public TextField textFieldORE;
    public TextField textFieldODU;
    public TextField textFieldVersion;
    public TextField textFieldSavePath;
    public TextField textFieldNumber;
    public TextField textFieldName;
    public RadioButton radioButtonWinter;
    public RadioButton radioButtonSummer;
    public CheckBox checkBoxAutoSave;
    public Button btnSelectSaveDir;

    private Document settingsXmlDoc;

    public Document getSettingsXmlDoc() {
        return settingsXmlDoc;
    }

    private final String fileName = "settings.xml";

    public String getFileName() {
        return fileName;
    }

    public String getName() {
        return textFieldName.getText();
    }

    @FXML
    // заполнение окна настроек данными из файла настроек
    public void initialize() {
        // помещаем радио кнопки в одну группу выбора
        ToggleGroup toggleGroup = new ToggleGroup();
        radioButtonWinter.setToggleGroup(toggleGroup);
        radioButtonSummer.setToggleGroup(toggleGroup);

        try {
            File dir = new File(appDataDir);
            // проверяем наличие польз. папки appDataDir
            // если ее нет, то пытаемся создать ее и извлечь в нее из ресурсов файлы settings.xml и ORE.txt
            if (!Files.exists(dir.toPath())) {
                messageWindow.showModalWindow("Внимание", "Не удалось найти настройки программы. " +
                "Возможно это первый запуск. Будут созданы настройки по умолчанию.", Alert.AlertType.WARNING);
                if (!dir.mkdir()) {
                    messageWindow.showModalWindow("Ошибка", "Не удалость создать папку "+
                            appDataDir + ". Программа будет закрыта!", Alert.AlertType.ERROR);
                    //завершаем работу программы
                    Platform.exit();
                    System.exit(0);
                }
                resourceToFile("/Resources/settings.xml", appDataDir + slash + fileName);
                resourceToFile("/Resources/ORE.txt", appDataDir + slash + "ORE.txt");
            }
            File settings = new File(appDataDir + slash +fileName);
            File ore = new File(appDataDir + slash + "ORE.txt");

            // проверяем наличие файлов settings.xml и ORE.txt в папке ASKUE
            // к этому моменту проверка существования папки ASKUE пройдена
            if (!Files.exists(settings.toPath())) {
                resourceToFile("/Resources/settings.xml", appDataDir + slash + fileName);
                messageWindow.showModalWindow("Внимание", "Не удалось найти файл с настройками программы." +
                                " Были созданы настройки по умолчанию!",
                        Alert.AlertType.WARNING);
            }
            if (!Files.exists(ore.toPath())) {
                resourceToFile("/Resources/ORE.txt", appDataDir + slash + "ORE.txt");
                messageWindow.showModalWindow("Внимание", "Не удалось найти файл с кодами контрагентов ОРЭ." +
                                " Были созданы настройки по умолчанию!", Alert.AlertType.WARNING);
            }

            settingsXmlDoc = XmlUtil.getXmlDoc(settings.toURI().toURL());
            XmlUtil.removeWhitespaceNodes(settingsXmlDoc.getDocumentElement());

            textFieldName.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("name").
                    getNodeValue());
            textFieldINN.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("INN").
                    getNodeValue());
            textFieldAIIS.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("aiis_code").
                    getNodeValue());
            textFieldORE.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("ore_code").
                    getNodeValue());
            textFieldODU.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("ODU").
                    getNodeValue());
            textFieldVersion.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("version").
                    getNodeValue());
            textFieldNumber.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("number").
                    getNodeValue());
            textFieldSavePath.setText(settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("savepath").
                    getNodeValue());
            String dayLightSavingTime = settingsXmlDoc.getDocumentElement().getAttributes().
                    getNamedItem("DayLightSavingTime").getNodeValue();
            String autoSave = settingsXmlDoc.getDocumentElement().getAttributes().
                    getNamedItem("autosave").getNodeValue();
            if (autoSave.equals("1")) {
                checkBoxAutoSave.setSelected(true);
            } else
            {
                textFieldSavePath.setDisable(true);
                btnSelectSaveDir.setDisable(true);
            }
            if (dayLightSavingTime.equals("0")) {
                radioButtonWinter.setSelected(true);
            } else {
                radioButtonSummer.setSelected(true);
            }
            // на поле с номером вешаем слушателя для ввода только цифр в него
            textFieldNumber.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    String val = newValue.replaceAll("\\D+", "");
                    if (!val.equals("")) textFieldNumber.setText(val); else
                        textFieldNumber.setText(oldValue);
                }
            });
            // на чекбокс вешаем слушателя выбора/снятия флажка
            checkBoxAutoSave.selectedProperty().addListener(event -> {
                if (checkBoxAutoSave.isSelected()) {
                    textFieldSavePath.setDisable(false);
                    btnSelectSaveDir.setDisable(false);
                } else
                {
                    textFieldSavePath.setDisable(true);
                    btnSelectSaveDir.setDisable(true);
                }
            });
        }
        catch (FileNotFoundException e1) {
            messageWindow.showModalWindow("Ошибка", "Невозможно загрузить настройки. Программа будет закрыта. "+
                    "Проверьте наличие файла settings.xml", Alert.AlertType.ERROR);
            //завершаем работу программы
            Platform.exit();
            System.exit(0);

        }
        catch (Exception e2) {
            messageWindow.showModalWindow("Ошибка", e2.getMessage(), Alert.AlertType.ERROR);
            Platform.exit();
            System.exit(0);
        }
    }

    // закрытие окна настроек
    public void closeWindow(ActionEvent actionEvent) {
        if (textFieldNumber.getText().length() == 0) {
            messageWindow.showModalWindow("Внимание", "Номер файла не должен быть пустым",
                    Alert.AlertType.INFORMATION);
            return;
        }
        Node node = ((Node)actionEvent.getSource());
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }

    // выбор папки для автосохранения макетов
    public void selectSaveDir(ActionEvent actionEvent) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Выберите папку для сохранения макетов");
        File path = dirChooser.showDialog(new Stage());
        if (path != null)
            textFieldSavePath.setText(path.getAbsolutePath());
    }

    public void saveNumber(String newNumber) {

        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("number").
                setNodeValue(newNumber);
        try {
            // форматируем и сохраняем документ в xml-файл с кодировкой windows-1251
            XmlUtil.saveXMLDoc(settingsXmlDoc, appDataDir + slash + fileName,"windows-1251",
                    true);
        }
        catch (TransformerException e) {
            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + fileName +
                    " завершена неудачно!", Alert.AlertType.ERROR);
        }
    }

    // сохранение настроек в файл /Templates/settings.xml
    public void saveSettings(ActionEvent actionEvent) {
        if (textFieldNumber.getText().length() == 0) {
            messageWindow.showModalWindow("Внимание", "Номер файла не должен быть пустым",
                    Alert.AlertType.INFORMATION);
            return;
        }
        XmlUtil.removeWhitespaceNodes(settingsXmlDoc.getDocumentElement());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("name").
                setNodeValue(textFieldName.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("INN").
                setNodeValue(textFieldINN.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("version").
                setNodeValue(textFieldVersion.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("aiis_code").
                setNodeValue(textFieldAIIS.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("ore_code").
                setNodeValue(textFieldORE.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("ODU").
                setNodeValue(textFieldODU.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("number").
                setNodeValue(textFieldNumber.getText());
        settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("savepath").
                setNodeValue(textFieldSavePath.getText());

        if (radioButtonWinter.isSelected())
            settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("DayLightSavingTime").
                setNodeValue("0"); else
                    settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("DayLightSavingTime").
                        setNodeValue("1");
        if (checkBoxAutoSave.isSelected())
            settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("autosave").
                setNodeValue("1"); else
            settingsXmlDoc.getDocumentElement().getAttributes().getNamedItem("autosave").
                    setNodeValue("0");

        try {
            // форматируем и сохраняем документ в xml-файл с кодировкой windows-1251
            XmlUtil.saveXMLDoc(settingsXmlDoc, appDataDir + slash + fileName,"windows-1251",
                    true);
            closeWindow(actionEvent);
        }
        catch (TransformerException e) {
            messageWindow.showModalWindow("Ошибка", "Трансформация в файл " + fileName +
                    " завершена неудачно!", Alert.AlertType.ERROR);
        }
    }
}
