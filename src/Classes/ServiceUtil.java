package Classes;

import javafx.scene.control.Alert;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ServiceUtil {
    public static MessageWindow messageWindow = new MessageWindow(Alert.AlertType.INFORMATION);
    public static TextWindow textWindow = new TextWindow(Alert.AlertType.ERROR);

    // метод возвращает строку текущего времени в формате yyyyMMddHHmmss
    public static String getCurrentDateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return simpleDateFormat.format(new Date());
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
                String fileName = file.getName();
                if (fileName.length() < 38)
                    throw new FileNameException();

                String fileNameClass = fileName.substring(0, 5);
                if (!(fileNameClass.contains("80020") || fileNameClass.contains("80040") ||
                        fileNameClass.contains("80025")))
                    throw new FileNameException();

                measPointCount = xmlDoc.getDocumentElement().getElementsByTagName("measuringpoint").getLength();
                if (measPointCount == 0) {
                    throw new NoMeasPointException();
                }

                if (xmlDoc.getDocumentElement().getElementsByTagName("area").item(0) == null &&
                        xmlDoc.getDocumentElement().getElementsByTagName("aiis").item(0) == null) {
                    throw new NoAreaException();
                }

            }

            catch (FileNameException e1) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Имя импортируемого файла " +
                        "не соответствует требуемым параметрам");
            }
            catch (NoMeasPointException e2 ) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Отсутствуют точки измерения");
            }
            catch (NoAreaException e3 ) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Отсутствует area или aiis");
            }
            catch (SAXParseException e4) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Ошибка разбора XML. Строка: " +
                        e4.getLineNumber() + ", символ: " + e4.getColumnNumber());
            }

            catch (ParserConfigurationException e5 ) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Серьезная ошибка конфигурации парсера");
            }
            catch (SAXException e6) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Общая ошибка парсера");
            }
            catch (IOException e7) {
                validFileList.remove(file);
                failedFileList.put(file.getName(), "Ошибка ввода/вывода. Проверьте доступность файла XML");
            }
        }
        String text="";
        for (Map.Entry<String, String> pair: failedFileList.entrySet()) {
            text += pair.getKey() + ": "+ pair.getValue() + "." + System.lineSeparator();
        }

        if (failedFileList.size() > 0) {
            textWindow.showModalWindow("Ошибка", "Следующие файлы содержат ошибки и не будут загружены",
                    text, Alert.AlertType.ERROR);
        }
        return validFileList;
    }

    // метод распаковывает файл с названием fileName из jar-архива file
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
}
