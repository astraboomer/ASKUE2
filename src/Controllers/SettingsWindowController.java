package Controllers;

import Classes.XML80020;
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
import java.io.File;
import java.io.FileNotFoundException;

import static Classes.XmlClass.alertWindow;


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
    public RadioButton checkBoxWinter;
    public RadioButton checkBoxSummer;
    public CheckBox checkBoxAutoSave;

    private Document xmlNewDoc;

    public String getName() {
        return textFieldName.getText();
    }

    @FXML
    // заполнение окна настроек данными из файла настроек
    public void initialize() {
        File settings = new File(System.getProperty("user.dir")+"/src/Templates/settings.xml");
        try {
            xmlNewDoc = XmlUtil.getXmlDoc(settings);

            textFieldName.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("name").
                    getNodeValue());
            textFieldINN.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("INN").
                    getNodeValue());
            textFieldAIIS.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("aiis_code").
                    getNodeValue());
            textFieldORE.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("ore_code").
                    getNodeValue());
            textFieldODU.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("ODU").
                    getNodeValue());
            textFieldVersion.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("version").
                    getNodeValue());
            textFieldNumber.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("number").
                    getNodeValue());
            textFieldSavePath.setText(xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("savepath").
                    getNodeValue());
            String dayLightSavingTime = xmlNewDoc.getDocumentElement().getAttributes().
                    getNamedItem("DayLightSavingTime").getNodeValue();
            String autoSave = xmlNewDoc.getDocumentElement().getAttributes().
                    getNamedItem("autosave").getNodeValue();
            if (autoSave.equals("1"))
                checkBoxAutoSave.setSelected(true);
            if (dayLightSavingTime.equals("0")) {
                checkBoxWinter.setSelected(true);
            } else {
                checkBoxSummer.setSelected(true);
            }
            // на поле с номером вешаем слушателя для ввода только цифр в него
            textFieldNumber.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    String val = newValue.replaceAll("\\D+", "");

                    if (!val.equals("")) textFieldNumber.setText(val); else
                        textFieldNumber.setText(oldValue);
                }
            });
        }
        catch (FileNotFoundException e1) {
            alertWindow.setAlertType(Alert.AlertType.ERROR);
            alertWindow.setTitle("Ошибка");
            alertWindow.setHeaderText(null);
            alertWindow.setContentText("Невозможно загрузить настройки. Программа будет закрыта "+
                    "Проверьте наличие файла " + settings.getAbsolutePath());
            alertWindow.showAndWait();
            //завершаем работу программы
            Platform.exit();
            System.exit(0);

        }
        catch (Exception e2) {
            alertWindow.setAlertType(Alert.AlertType.ERROR);
            alertWindow.setTitle("Ошибка");
            alertWindow.setHeaderText(null);
            alertWindow.setContentText(e2.getLocalizedMessage());
            alertWindow.showAndWait();
            Platform.exit();
            System.exit(0);
        }
    }

    // закрытие окна настроек
    public void closeWindow(ActionEvent actionEvent) {
        Node node = ((Node)actionEvent.getSource());
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }

    // обрабатываем переключения radioButton-ов
    public void selectSeasonTime(ActionEvent actionEvent) {
        RadioButton radioButton = (RadioButton) actionEvent.getSource();
        radioButton.setSelected(true);
        if (radioButton.getText().equals("Зимнее время"))
            checkBoxSummer.setSelected(false); else
                checkBoxWinter.setSelected(false);
    }
    // выбор папки для автосохранения макетов
    public void selectSaveDir(ActionEvent actionEvent) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Выберите папку для сохранения макетов");
        File path = dirChooser.showDialog(new Stage());
        if (path != null)
            textFieldSavePath.setText(path.getAbsolutePath());

    }

    public void saveSettings(ActionEvent actionEvent) {
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("name").
                setNodeValue(textFieldName.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("INN").
                setNodeValue(textFieldINN.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("version").
                setNodeValue(textFieldVersion.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("aiis_code").
                setNodeValue(textFieldAIIS.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("ore_code").
                setNodeValue(textFieldORE.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("ODU").
                setNodeValue(textFieldODU.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("number").
                setNodeValue(textFieldNumber.getText());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("savepath").
                setNodeValue(textFieldSavePath.getText());

        if (checkBoxWinter.isSelected())
            xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("DayLightSavingTime").
                setNodeValue("0"); else
                    xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("DayLightSavingTime").
                        setNodeValue("1");
        if (checkBoxAutoSave.isSelected())
            xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("autosave").
                setNodeValue("1"); else
            xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("autosave").
                    setNodeValue("0");

        try {
            // форматируем и сохраняем документ в xml-файл с кодировкой windows-1251
            XmlUtil.saveXMLDoc(xmlNewDoc, System.getProperty("user.dir")+"/src/Templates/settings.xml",
                    "windows-1251", true);
            closeWindow(actionEvent);
        }
        catch (TransformerException e) {
            alertWindow.setAlertType(Alert.AlertType.ERROR);
            alertWindow.setTitle("Ошибка");
            alertWindow.setHeaderText(null);
            alertWindow.setContentText("Трансформация в файл " + System.getProperty("user.dir")+
                    "/src/Templates/settings.xml" +" завершена неудачно!");
            alertWindow.showAndWait();
        }
    }
}
