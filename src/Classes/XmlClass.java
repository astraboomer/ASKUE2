package Classes;

import Classes.XmlTag.DateTime;
import Classes.XmlTag.Message;
import Classes.XmlTag.Sender;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Сергей on 10.05.2017.
 */
public abstract class XmlClass {
    private File file;
    private Message message;
    private DateTime dateTime;
    private Sender sender;
    public static MessageWindow messageWindow = new MessageWindow(Alert.AlertType.INFORMATION);
    public static Alert errorFilesWindow = new Alert(Alert.AlertType.ERROR);

    public File getFile() {
        return file;
    }

    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }



    public XmlClass (File file) {
        this.file = file;
    }
    /*
    Метод проверяет список выбранных xml-файлов доступность и корректность структуры XML.
    Для этого все выбранные файлы помещаются в список validFileList и, если не
    удается разобрать файл или он недоступен, то этот файл удаляется из списка.
    Возвращается список validFileList с корректными xml-файлами. Если список validFileList
    не пуст, то список файлов с описанием ошибок выводится в новом окне.
     */
    public static List<File> validateXMLFiles(List<File> fileList) {
        List<File> validFileList = new ArrayList<>();
        Map<String, String> failedFileList = new HashMap<>();
        validFileList.addAll(fileList);
        int measPointCount;
        for (File file: fileList) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document xmlDoc = db.parse(file);
                measPointCount = xmlDoc.getDocumentElement().getElementsByTagName("measuringpoint").getLength();
                if (measPointCount == 0) {
                    throw new NoMeasPointException();
                }
            }

            catch (NoMeasPointException e0 ) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Отсутствуют точки измерения");
            }
            catch (SAXParseException e1) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Ошибка разбора XML. Строка: " +
                e1.getLineNumber() + ", символ: " + e1.getColumnNumber());
            }

            catch (ParserConfigurationException e2 ) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Серьезная ошибка конфигурации парсера");
            }
            catch (SAXException e3) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Общая ошибка парсера");
            }
            catch (IOException e4) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Ошибка ввода/вывода. Проверьте доступность файла XML");
            }
        }
        String text="";
        for (Map.Entry<String, String> pair: failedFileList.entrySet()) {
            text += pair.getKey() + ": "+ pair.getValue() + "." + System.lineSeparator();
        }

        if (failedFileList.size() > 0) {
            errorFilesWindow.setTitle("Ошибка");
            errorFilesWindow.setHeaderText(null);
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

            errorFilesWindow.getDialogPane().setHeaderText("Следующие файлы содержат ошибки и не будут загружены");
            errorFilesWindow.getDialogPane().setContent(expContent);
            errorFilesWindow.showAndWait();
        }
        return validFileList;
    }

    // метод возвращает строку текущего времени в формате yyyyMMddHHmmss
    public static String getCurrentDateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return simpleDateFormat.format(new Date());
    }

}
