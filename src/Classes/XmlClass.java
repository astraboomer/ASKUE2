package Classes;

import org.w3c.dom.Document;
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

/**
 * Created by Сергей on 10.05.2017.
 */
public class XmlClass {
    private String version;
    private String encoding;
    private String standalone;
    private Document xmlDOMDoc;

    public XmlClass (File file) {
        this.file = file;
        try {
            loadXmlDOMDoc();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getStandalone() {
        return standalone;
    }

    public void setStandalone(String standalone) {
        this.standalone = standalone;
    }

    private File file;
    public File getFile() {
        return file;
    }

    // создает DOM-документ, с которым будет вестись вся дальнейшая работа, из xml-файла
    public void loadXmlDOMDoc () throws IOException, SAXException,
            ParserConfigurationException {
        DocumentBuilderFactory dbf;
        DocumentBuilder db;
        dbf = DocumentBuilderFactory.newInstance();
        // игнорируем все возможные пробелы
        dbf.setIgnoringElementContentWhitespace(true);
        // включаем поддержку пространства имен
        dbf.setNamespaceAware(true);
        db  = dbf.newDocumentBuilder();
        xmlDOMDoc = db.parse(file.toURI().toURL().openStream());
        if (!xmlDOMDoc.getXmlStandalone())
            this.standalone = "no";
        else
            this.standalone = "yes";

        this.encoding = xmlDOMDoc.getXmlEncoding();
        this.version = xmlDOMDoc.getXmlVersion();
        xmlDOMDoc.getDocumentElement().normalize();
    }

    public Document getXmlDOMDoc() {
        return xmlDOMDoc;
    }

    // метод сохраняет xmlDOMDoc в файл fileName с кодировкой encoding и необходимостью форматирования
    public void saveXMLDOMDoc (String fileName, boolean needFormat)
            throws TransformerException {
        XmlUtil.removeWhitespaceNodes(xmlDOMDoc.getDocumentElement());
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.setOutputProperty(OutputKeys.STANDALONE, standalone);
        File file = new File(fileName);
        if (needFormat) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }
        transformer.transform(new DOMSource(xmlDOMDoc), new StreamResult(file));
    }

}
