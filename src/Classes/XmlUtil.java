package Classes;

/**
 * Вспомогательный класс для работы с любым XML-файлом
 */
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public final class XmlUtil {
    // создает новый DOM-документ
    public static Document createXmlDoc() throws IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xmlDoc = db.newDocument();
        xmlDoc.setXmlStandalone(true);
        return xmlDoc;
    }

    // возвращает DOM-документ xml-файла с которым будет вестись вся дальнейшая работа
    public static Document getXmlDoc (URL xmlFile) throws  IOException, SAXException,
    ParserConfigurationException{
        DocumentBuilderFactory dbf;
        DocumentBuilder db;
        dbf = DocumentBuilderFactory.newInstance();
        // игнорируем все возможные пробелы
        dbf.setIgnoringElementContentWhitespace(true);
        // включаем поддержку пространства имен
        dbf.setNamespaceAware(true);
        db  = dbf.newDocumentBuilder();
        Document xmlDoc = db.parse(xmlFile.openStream());
        xmlDoc.getDocumentElement().normalize();
        return xmlDoc;
    }

    // метод сохраняет xmlDoc в файл fileName с кодировкой encoding и необходимостью форматирования
    public static void saveXMLDoc (Document xmlDoc, String fileName, String encoding, boolean needFormat)
    throws TransformerException {
            removeWhitespaceNodes(xmlDoc.getDocumentElement());
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            File file = new File(fileName);
            if (needFormat) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            transformer.transform(new DOMSource(xmlDoc), new StreamResult(file));
    }

    // метод удаляет все тексовые (пустые) узлы и пробелы из переданного узла DOM-дерева
    // необходим для коррект. форматирования при сохранении документа
    public static void removeWhitespaceNodes(Element e) {
        NodeList children = e.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child instanceof Text && ((Text) child).getData().trim().length() == 0) {
                e.removeChild(child);
            } else if (child instanceof Element) {
                removeWhitespaceNodes((Element) child);
            }
        }
    }

}
