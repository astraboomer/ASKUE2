package Classes;

import Classes.XmlTag.*;
import javafx.scene.control.Alert;
import org.w3c.dom.*;

import javax.xml.transform.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static Classes.ServiceUtil.messageWindow;

/**
 * Created by Сергей on 12.05.2017.
 */
public class XML80020 extends XmlClass {
    private Message message;
    private DateTime datetime;
    private Sender sender;
    private List<Area> areaList = new ArrayList<>();

    public XML80020(File file) {
        super(file);
        loadDataFromXML();
    }

    public Message getMessage() {
        return message;
    }

    public DateTime getDateTime() {
        return datetime;
    }

    public Sender getSender() {
        return sender;
    }

    public List<Area> getAreaList() {
        return areaList;
    }

    void setMessage(Message message) {
        this.message = message;
    }

    void setDateTime(DateTime datetime) {
        this.datetime = datetime;
    }

    void setSender(Sender sender) {
        this.sender = sender;
    }

    public void addArea(Area area) {
        this.areaList.add(area);
    }

    /*
     Получение поля message этого класса из xml-документа
     */
    public void readMessage() {
        Message message = new Message();
        message.setMessageClass(getXmlDOMDoc().getDocumentElement().getAttributes().getNamedItem("class").getNodeValue());
        message.setVersion(getXmlDOMDoc().getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
        message.setNumber(getXmlDOMDoc().getDocumentElement().getAttributes().getNamedItem("number").getNodeValue());
        this.message = message; // переменной message этого класса передаем через метод значение локал. message-а
    }

    /*
    Получение поля dateTime этого класса из xml-документа
     */
    public void readDateTime() {
        DateTime dateTime = new DateTime();
        NodeList messageChildNodeList = getXmlDOMDoc().getDocumentElement().getChildNodes();
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
        this.datetime = dateTime; // переменной dateTime этого класса передаем через метод значение локал. dateTime-а
    }

    /*
    Получение поля sender этого класса из xml-документа
     */
    public void readSender() {
        Sender sender = new Sender();
        NodeList messageChildNodeList = getXmlDOMDoc().getDocumentElement().getChildNodes();
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
        this.sender = sender; // переменной этого класса sender передаем через метод значение локал. sender-а
    }

    /*
    Получение поля measChannelList для measuringpoint по переданному узлу
    measuringpoint, а также определение содержится ли хотя бы одно некоммер. значение
    в канале. Если содержится, то помечаем это через метод setCommercialInfo(false)
     */
    public List<MeasuringChannel> readMeasChannelList(Node measPointNode) {
        List<MeasuringChannel> measChannelList = new ArrayList<>();
        NodeList measPointChildNodeList = measPointNode.getChildNodes();
        int measPointChildNodeCount = measPointChildNodeList.getLength();
        for (int i = 0; i < measPointChildNodeCount; i++) { // перебираем все дочер. узлы measuringpoint-а
            if (measPointChildNodeList.item(i).getNodeName().equals("measuringchannel")) {
                MeasuringChannel measuringChannel = new MeasuringChannel();
                measuringChannel.setCode(measPointChildNodeList.item(i).getAttributes().getNamedItem("code").
                        getNodeValue());
                measuringChannel.setAliasName(measuringChannel.getAliasNameChannelByCode(measuringChannel.getCode()));
                Node descAttr = measPointChildNodeList.item(i).getAttributes().getNamedItem("desc");
                if (descAttr != null)
                    measuringChannel.setDesc(descAttr.getNodeValue());
                NodeList measChannelChildNodeList = measPointChildNodeList.item(i).getChildNodes();
                int measChannelChildNodeCount = measChannelChildNodeList.getLength();
                for (int j = 0; j < measChannelChildNodeCount; j++) { // перебираем все дочер. узлы measuringchannel-а
                    if (measChannelChildNodeList.item(j).getNodeName().equals("period")) {
                        Period period = new Period();
                        period.setStart(measChannelChildNodeList.item(j).getAttributes().getNamedItem("start").getNodeValue());
                        period.setEnd(measChannelChildNodeList.item(j).getAttributes().getNamedItem("end").getNodeValue());
                        period.setInterval();
                        Node value = measChannelChildNodeList.item(j).getChildNodes().item(0);
                        period.setValue(value.getTextContent());
                        // проверяем наличие атрибута статус у узла value
                        Node statusAttr = value.getAttributes().getNamedItem("status");
                        if (statusAttr != null) { // атрибут status действительно имеется
                            period.setStatus(statusAttr.getNodeValue());
                            if (period.getStatus().equals("1")) {
                                measuringChannel.setCommercialInfo(false);
                            }
                        }
                        Node extStatusAttr = value.getAttributes().getNamedItem("extendedstatus");
                        if (extStatusAttr != null) { // атрибут extendedstatus действительно имеется
                            period.setExtendedStatus(extStatusAttr.getNodeValue());
                        }
                        Node param1Attr = value.getAttributes().getNamedItem("param1");
                        if (param1Attr != null) { // атрибут param1 действительно имеется
                            period.setParam1(param1Attr.getNodeValue());
                        }
                        measuringChannel.addPeriod(period);
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
    public MeasuringPoint readMeasurePoint(Node measPointNode) {
        MeasuringPoint measuringPoint = new MeasuringPoint();
        measuringPoint.setCode(measPointNode.getAttributes().getNamedItem("code").getNodeValue());
        measuringPoint.setName(measPointNode.getAttributes().getNamedItem("name").getNodeValue());
        // для каждого переданного узла measuringpoint получаем список measuringchannel-ов
        List<MeasuringChannel> measChannelList = readMeasChannelList(measPointNode);
        // через метод класса передаем список measuringchannel-ов этому measuringpoint-у
        measuringPoint.setMeasChannelList(measChannelList);
        return measuringPoint;
    }

    /*
    Получение списка элементов area из xml-документа
     */
    public void readAreaList() {
        // получаем все узлы area из xml-документа
        NodeList areaNodeList = getXmlDOMDoc().getDocumentElement().getElementsByTagName("area");
        int areaNodeCount = areaNodeList.getLength();
        for (int i = 0; i < areaNodeCount; i++) { // перебираем все узлы area
            Area area = new Area();
            Node timezone = areaNodeList.item(i).getAttributes().getNamedItem("timezone");
            if (timezone != null)
                area.setTimeZone(timezone.getNodeValue());
            // получаем список дочерних узлов i-й area
            NodeList areaChildNodeList = areaNodeList.item(i).getChildNodes();
            int areaChildNodeCount = areaChildNodeList.getLength();
            for (int j = 0; j < areaChildNodeCount; j++) { // перебираем все дочерние узлы узла area
                if (areaChildNodeList.item(j).getNodeName().equals("inn")) {
                    area.setInn(areaChildNodeList.item(j).getTextContent());
                    continue;
                }
                if (areaChildNodeList.item(j).getNodeName().equals("name")) {
                    area.setName(areaChildNodeList.item(j).getTextContent());
                    continue;
                }
                if (areaChildNodeList.item(j).getNodeName().equals("measuringpoint")) {
                    // получаем элемент measPointList-а по узлу measuringpoint
                    MeasuringPoint measuringPoint = readMeasurePoint(areaChildNodeList.item(j));
                    // добавляем этот элемент в список measPointList объекта area
                    area.addMeasPoint(measuringPoint);
                    // добавляем этот узел measuringpoint в список measPointNodeList объекта area
                }
            }
            // в спикок areaList добавляем area
            this.addArea(area);
        }
    }

    // загружаем данные из текущего xml-файла
    public void loadDataFromXML() {
        // получаем xml-документ в случае успешного разбора xml-файла
        // заполнение полей класса данными из xml-документа
        Document xmlDoc;
        try {
            loadXmlDOMDoc();
            xmlDoc = getXmlDOMDoc();
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
        readMessage();
        readDateTime();
        readSender();
        readAreaList();
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
        catch (Exception e1) {
            messageWindow.showModalWindow("Ошибка", e1.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        // заносим новые значения атрибутов корневого узла message
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("class").setTextContent(getMessage().
                getMessageClass());
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("version").setTextContent(messVersion);
        xmlNewDoc.getDocumentElement().getAttributes().getNamedItem("number").setTextContent(messNumber);

        // заносим новые значения узлов
        xmlNewDoc.getDocumentElement().getElementsByTagName("timestamp").item(0).
                setTextContent(ServiceUtil.getCurrentDateTime());
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
                    Element measPointNode = xmlNewDoc.createElement("measuringpoint");
                    measPointNode.setAttribute("code", measPointList.get(i).getCode());
                    measPointNode.setAttribute("name", measPointList.get(i).getName());
                    List<MeasuringChannel> measChannelList = measPointList.get(i).getMeasChannelList();
                    for (MeasuringChannel measuringChannel: measChannelList)
                        if (measuringChannel.isSelected()) {
                            Element measChannelNode = xmlNewDoc.createElement("measuringchannel");
                            measChannelNode.setAttribute("code", measuringChannel.getCode());
                            if (measuringChannel.getDesc() != null)
                                measChannelNode.setAttribute("desc", measuringChannel.getDesc());
                            List<Period> periodList = measuringChannel.getPeriodList();
                            for (Period period: periodList) {
                                Element periodNode = xmlNewDoc.createElement("period");
                                periodNode.setAttribute("start", period.getStart());
                                periodNode.setAttribute("end", period.getEnd());
                                Element valueNode = xmlNewDoc.createElement("value");
                                valueNode.setTextContent(period.getValue());
                                if (period.getStatus() != null && period.getStatus().equals("1")) {
                                    valueNode.setAttribute("status", period.getStatus());
                                }
                                if (period.getExtendedStatus() != null) {
                                    valueNode.setAttribute("extendedstatus", period.getExtendedStatus());
                                }
                                if (period.getParam1() != null) {
                                    valueNode.setAttribute("param1", period.getParam1());
                                }
                                periodNode.appendChild(valueNode);
                                measChannelNode.appendChild(periodNode);
                            }
                            measPointNode.appendChild(measChannelNode);
                        }
                        areaNode.appendChild(measPointNode);
                }
            }
        }
        XmlUtil.saveXMLDoc(xmlNewDoc, outFileName, "windows-1251", true);
        //xmlNewDoc. .saveXMLDOMDoc(outFileName, true);
    }
}
