package Classes;

import Classes.XmlTag.*;
import Controllers.MainWindowController;
import Controllers.SettingsWindowController;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static Classes.Main.slash;

/**
 * Created by Сергей on 12.05.2017.
 */
public class XML80020 extends XmlClass {
    public static final String activeInput = "Активная энергия, прием";
    public static final String activeOutput = "Активная энергия, отдача";
    public static final String reactiveInput = "Реактивная энергия, прием";
    public static final String reactiveOutput = "Реактивная энергия, отдача";

    public List<Area> areaList;
    public List<Area> getAreaList() {
        return areaList;
    }
    public XML80020(File file) {
        super(file);
    }
    /*
    Получение поля message этого класса из xml-документа
     */
    private void setMessage (Document xmlDoc) {
        Message message = new Message();
        message.setMessageClass(xmlDoc.getDocumentElement().getAttributes().getNamedItem("class").getNodeValue());
        message.setVersion(xmlDoc.getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
        message.setNumber(xmlDoc.getDocumentElement().getAttributes().getNamedItem("number").getNodeValue());
        this.setMessage(message); // переменной message этого класса передаем через метод значение локал. message-а
    }
    /*
    Получение поля dateTime этого класса из xml-документа
     */
    private void setDateTime (Document xmlDoc) {
        DateTime dateTime = new DateTime();
        NodeList messageChildNodeList = xmlDoc.getDocumentElement().getChildNodes();
        int messageChildNodeCount = messageChildNodeList.getLength();
        for (int i = 0; i < messageChildNodeCount; i++) { // перебираем все дочер. узлы message-а
            if (messageChildNodeList.item(i).getNodeName().equals("datetime")) {
                NodeList dtChildNodeList = messageChildNodeList.item(i).getChildNodes();
                int dtChildNodeCount = dtChildNodeList.getLength();
                for (int j = 0; j < dtChildNodeCount; j++) { // перебираем все дочер. узлы datetime-а
                    if (dtChildNodeList.item(j).getNodeName().equals("timestamp")) {
                        dateTime.setTimeStamp(dtChildNodeList.item(j).getTextContent());
                    }
                    if (dtChildNodeList.item(j).getNodeName().equals("daylightsavingtime")) {
                        dateTime.setDaylightSavingTime(dtChildNodeList.item(j).getTextContent());
                    }
                    if (dtChildNodeList.item(j).getNodeName().equals("day")) {
                        dateTime.setDay(dtChildNodeList.item(j).getTextContent());
                    }
                }
                break; // выходим из цикла, если datetime найден
            }
        }
        this.setDateTime(dateTime); // переменной dateTime этого класса передаем через метод значение локал. dateTime-а
    }
    /*
    Получение поля sender этого класса из xml-документа
     */
    private void setSender (Document xmlDoc) {
        Sender sender = new Sender();
        NodeList messageChildNodeList = xmlDoc.getDocumentElement().getChildNodes();
        int messageChildNodeCount = messageChildNodeList.getLength();
        for (int i = 0; i < messageChildNodeCount; i++) { // перебираем все дочер. узлы message-а
            if (messageChildNodeList.item(i).getNodeName().equals("sender")) {
                NodeList senderChildNodeList = messageChildNodeList.item(i).getChildNodes();
                int senderChildNodeCount = senderChildNodeList.getLength();
                for (int j = 0; j < senderChildNodeCount; j++) { // перебираем все дочер. узлы sender-а
                    if (senderChildNodeList.item(j).getNodeName().equals("inn")) {
                        sender.setInn(senderChildNodeList.item(j).getTextContent());
                    }
                    if (senderChildNodeList.item(j).getNodeName().equals("name")) {
                        sender.setName(senderChildNodeList.item(j).getTextContent());
                    }
                }
                break; // выходим из цикла, если sender найден
            }
        }
        this.setSender(sender); // переменной этого класса sender передаем через метод значение локал. sender-а
    }

    /*
    Получение поля measChannelList для measuringpoint по переданному узлу
    measuringpoint, а также определение содержится ли хотя бы одно некоммер. значение
    в канале. Если содержится, то помечаем это через метод setCommercialInfo(false)
     */
    public List<MeasuringChannel> setMeasChannelList(Node measPointNode) {
        List<MeasuringChannel> measChannelList = new ArrayList<>();
        NodeList measPointChildNodeList = measPointNode.getChildNodes();
        int measPointChildNodeCount = measPointChildNodeList.getLength();
        for (int i = 0; i < measPointChildNodeCount; i++) { // перебираем все дочер. узлы measuringpoint-а
            if (measPointChildNodeList.item(i).getNodeName().equals("measuringchannel")) {
                MeasuringChannel measuringChannel = new MeasuringChannel();
                measuringChannel.setCode(measPointChildNodeList.item(i).getAttributes().getNamedItem("code").
                        getNodeValue());
                measuringChannel.setAliasName(getAliasNameChannelByCode(measuringChannel.getCode()));
                /*measuringChannel.setDesc(measPointChildNodeList.item(i).getAttributes().getNamedItem("desc").
                        getNodeValue());*/
                NodeList measChannelChildNodeList = measPointChildNodeList.item(i).getChildNodes();
                int measChannelChildNodeCount = measChannelChildNodeList.getLength();
                for (int j = 0; j < measChannelChildNodeCount; j++) { // перебираем все дочер. узлы measuringchannel-а
                    if (measChannelChildNodeList.item(j).getNodeName().equals("period")) {
                        Node statusAttr = measChannelChildNodeList.item(j).getChildNodes().
                                item(0).getAttributes().getNamedItem("status");
                        if (statusAttr != null) { // атрибут status действительно имеется
                            if (statusAttr.getNodeValue().equals("1")) {
                                measuringChannel.setCommercialInfo(false);
                                break;
                            }
                        }
                    }
                }
                measChannelList.add(measuringChannel); // добавляем очередной measuringchannel в список
            }
        }
        return measChannelList; // возращаем список measuringchannel-ов
    }

    /*
    Получение элемента measPointList-а из xml-документа по узлу measuringpoint
     */
    public MeasuringPoint setMeasurePoint(Node measPointNode) {
        MeasuringPoint measuringPoint = new MeasuringPoint();
        measuringPoint.setCode(measPointNode.getAttributes().getNamedItem("code").getNodeValue());
        measuringPoint.setName(measPointNode.getAttributes().getNamedItem("name").getNodeValue());
        // для каждого переданного узла measuringpoint получаем список measuringchannel-ов
        List<MeasuringChannel> measChannelList = setMeasChannelList(measPointNode);
        // через метод класса передаем список measuringchannel-ов этому measuringpoint-у
        measuringPoint.setMeasChannelList(measChannelList);
        return measuringPoint;
    }

    /*
    Получение списка элементов area из xml-документа
     */
    private void setAreaList (Document xmlDoc) {
        List<Area> areaList = new ArrayList<>();
        // получаем все узлы area из xml-документа
        NodeList areaNodeList = xmlDoc.getDocumentElement().getElementsByTagName("area");
        int areaNodeCount = areaNodeList.getLength();
        for (int i = 0; i < areaNodeCount; i++) { // перебираем все узлы area
            Area area = new Area();
            Node timezone = areaNodeList.item(i).getAttributes().getNamedItem("timezone");
            if (timezone != null)
                area.setTimeZone(timezone.getNodeValue());

            NodeList areaChildNode = areaNodeList.item(i).getChildNodes();
            int areaChildNodeCount = areaChildNode.getLength();
            List<MeasuringPoint> measuringPointList = new ArrayList<>();
            List<Node> measPointNodeList = new ArrayList<>();
            for (int j = 0; j < areaChildNodeCount; j++) { // перебираем все дочерние узлы узла area
                if (areaChildNode.item(j).getNodeName().equals("inn")) {
                    area.setInn(areaChildNode.item(j).getTextContent());
                }
                if (areaChildNode.item(j).getNodeName().equals("name")) {
                    area.setName(areaChildNode.item(j).getTextContent());
                }
                if (areaChildNode.item(j).getNodeName().equals("measuringpoint")) {
                    // получаем элемент measPointList-а по узлу measuringpoint
                    MeasuringPoint measuringPoint = setMeasurePoint(areaChildNode.item(j));
                    // добавляем этот элемент в список measPointList объекта area
                    measuringPointList.add(measuringPoint);
                    // добавляем этот узел measuringpoint в список measPointNodeList объекта area
                    measPointNodeList.add(areaChildNode.item(j));
                }
            }
            area.setMeasPointNodeList(measPointNodeList);
            area.setMeasPointList(measuringPointList);
            areaList.add(area);
        }
        this.areaList = areaList; // переменной этого класса areaList передаем через метод значение локал. areaList-а
    }

    // получаем название канала по его последней цифре кода
    private String getAliasNameChannelByCode (String code) {
        char ch = code.charAt(1);
        String result = "Неизвестный канал";
        switch (ch) {
            case '1' : result = activeInput;
                break;
            case '2' : result = activeOutput;
                break;
            case '3' : result = reactiveInput;
                break;
            case '4' : result = reactiveOutput;
                break;
        }
        return result;
    }

    // загружаем данные из текущего xml-файла
    public void loadDataFromXML() {
        // получаем xml-документ в случае успешного разбора xml-файла
        // заполнение полей класса данными из xml-документа
        Document xmlDoc;
        try {
            xmlDoc = XmlUtil.getXmlDoc(getFile().toURI().toURL());
        }
        catch (FileNotFoundException e1) {
            messageWindow.showModalWindow("Ошибка", "Не найден файл " + getFile().getAbsoluteFile(),
                    Alert.AlertType.ERROR);
            return;
        }
        catch (Exception e2) {
            messageWindow.showModalWindow("Ошибка", e2.getMessage(), Alert.AlertType.ERROR);
            return;
        }
        // удаляем все пустые текстовые узлы дерева
        XmlUtil.removeWhitespaceNodes(xmlDoc.getDocumentElement());
        // заполняем поля данными
        setMessage(xmlDoc);
        setDateTime(xmlDoc);
        setSender(xmlDoc);
        setAreaList(xmlDoc);
    }

    // сохраняем данные с новыми параметрами
    public void saveDataToXML(String senderName, String senderINN, String areaName, String areaINN,
                              String messVersion, String messNumber, String newDLSavingTime,
                              String outFileName) throws TransformerException {
        URL template = this.getClass().getResource("/Resources/80020(40)_XML.xml");
        Document xmlNewDoc;
        // получаем xmlNewDoc из файла шаблона
        try {
            xmlNewDoc = XmlUtil.getXmlDoc(template);
            // удаляем все пустые текстовые узлы дерева
            XmlUtil.removeWhitespaceNodes(xmlNewDoc.getDocumentElement());
        }
        // если не удалось получить xmlNewDoc, то выходим из метода
        catch (FileNotFoundException e1) {
            messageWindow.showModalWindow("Ошибка", "Данные не сохранены! Отсутствует файл шаблона: " +
                    template.toString(), Alert.AlertType.ERROR);
            return;
        }
        catch (Exception e2) {
            messageWindow.showModalWindow("Ошибка", e2.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        // заносим новые значения атрибутов корневого узла message
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("class").setTextContent(getMessage().
                getMessageClass());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("version").setTextContent(messVersion);
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("number").
                setNodeValue(messNumber);

        // заносим новые значения узлов
        xmlNewDoc.getDocumentElement().getElementsByTagName("timestamp").item(0).
                setTextContent(XML80020.getCurrentDateTime());
        xmlNewDoc.getDocumentElement().getElementsByTagName("daylightsavingtime").item(0).
                setTextContent(newDLSavingTime);
        xmlNewDoc.getDocumentElement().getElementsByTagName("day").item(0).
                setTextContent(getDateTime().getDay());

        // заносим новые значения узлов
        Node sender = xmlNewDoc.getDocumentElement().getElementsByTagName("sender").item(0);
        sender.getChildNodes().item(0).setTextContent(senderINN);
        sender.getChildNodes().item(1).setTextContent(senderName);

        // из первой area берем значение timezone (хоть бы одна area всегда есть в документе)
        String timeZone = this.getAreaList().get(0).getTimeZone();

        // работаем с area
        Node areaNode = xmlNewDoc.getDocumentElement().getElementsByTagName("area").item(0);
        // если timezone есть (а ее может и не быть), то ставим ее значение в соотв. атрибут
        if (timeZone != null) {
            Element element = (Element) areaNode;
            element.setAttribute("timezone", timeZone);
        }
        areaNode.getChildNodes().item(0).setTextContent(areaINN);
        areaNode.getChildNodes().item(1).setTextContent(areaName);

        // перебираем все measurepoint-ы и если есть отмеченный, то смотрим его measurechannel-ы
        for (Area area : this.getAreaList()) {
            List<MeasuringPoint> measPointList = area.getMeasPointList();
            for (int i = 0; i < measPointList.size(); i++) {
                // если measurepoint отмечен копируем текущий measpoint в areaNode xmlNewDoc-а через
                // вспомогательный элемент, напрямую нельзя добавлять узел не из текущего xmlNewDoc-а
                if (measPointList.get(i).isSelected()) {
                    Element  copyNode = (Element) area.getMeasPointNodeList().get(i);
                    Element imported = (Element) xmlNewDoc.importNode(copyNode, true);
                    areaNode.appendChild(imported);

                    List<MeasuringChannel> measChannelList = measPointList.get(i).getMeasChannelList();
                    // проверяем в обратном порядке отмечены ли measchannel-ы и если да, то
                    // удаляем их из только что добавленного узла imported
                    for (int j = measChannelList.size() - 1; j >= 0; j--) {
                        if (!measChannelList.get(j).isSelected()) {
                            imported.removeChild(imported.getChildNodes().item(j));
                        }
                    }
                }
            }
        }
        XmlUtil.saveXMLDoc(xmlNewDoc, outFileName, "windows-1251", true);
    }

}
