package Classes;

import Classes.XmlTag.*;
import javafx.scene.control.Alert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static Classes.ServiceUtil.messageWindow;

public class XML80025 extends XML80020 {
    //private Area area;
    private Peretok peretok;

    public XML80025(File file) {
        super(file);
    }

    public Peretok getPeretok() {
        return peretok;
    }

    public void setPeretok(Peretok peretok) {
        this.peretok = peretok;
    }

    @Override
    public void readMessage() {
        Message message = new Message();
        message.setMessageClass(getXmlDOMDoc().getDocumentElement().getAttributes().getNamedItem("class").getNodeValue());
        message.setVersion(getXmlDOMDoc().getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
        setMessage(message); // переменной message этого класса передаем через метод значение локал. message-а
    }

    @Override
    public void readDateTime() {
        DateTime dateTime = new DateTime();
        NodeList messageChildNodeList = getXmlDOMDoc().getDocumentElement().getChildNodes();
        int messageChildNodeCount = messageChildNodeList.getLength();
        for (int i = 0; i < messageChildNodeCount; i++) { // перебираем все дочер. узлы message-а
            if (messageChildNodeList.item(i).getNodeName().equals("datetime")) {
                NodeList dtChildNodeList = messageChildNodeList.item(i).getChildNodes();
                int dtChildNodeCount = dtChildNodeList.getLength();
                for (int j = 0; j < dtChildNodeCount; j++) { // перебираем все дочер. узлы datetime-а
                    if (dtChildNodeList.item(j).getNodeName().equals("day")) {
                        dateTime.setDay(dtChildNodeList.item(j).getTextContent());
                        break;
                    }
                }
                break; // выходим из цикла, если datetime найден
            }
        }
        setDateTime(dateTime); // переменной dateTime этого класса передаем через метод значение локал. dateTime-а
    }

    @Override
    public void readAreaList () {
        //List<Area> areaList = new ArrayList<>();
        Sender sender = new Sender();
        NodeList messageChildNodeList = getXmlDOMDoc().getDocumentElement().getChildNodes();
        int messageChildNodeCount = messageChildNodeList.getLength();
        for (int i = 0; i < messageChildNodeCount; i++) { // перебираем все дочер. узлы message-а
            if (messageChildNodeList.item(i).getNodeName().equals("peretok")) {
                peretok = new Peretok();
                peretok.setCodeFrom(messageChildNodeList.item(i).getAttributes().getNamedItem("code-from").
                        getNodeValue());
                peretok.setCodeTo(messageChildNodeList.item(i).getAttributes().getNamedItem("code-to").
                        getNodeValue());
                peretok.setName(messageChildNodeList.item(i).getAttributes().getNamedItem("name").
                        getNodeValue());
                //this.setPeretok(peretok);
                NodeList peretokChildNodeList = messageChildNodeList.item(i).getChildNodes();
                int peretokChildNodeCount = peretokChildNodeList.getLength();
                for (int j = 0; j < peretokChildNodeCount; j++) { // перебираем все дочер. узлы peretok-а
                    if (peretokChildNodeList.item(j).getNodeName().equals("sender")) {
                        sender.setInn(peretokChildNodeList.item(j).getAttributes().getNamedItem("inn").getNodeValue());
                        sender.setName(peretokChildNodeList.item(j).getAttributes().getNamedItem("name").getNodeValue());
                        this.setSender(sender); // переменной этого класса sender передаем через метод значение
                                                // локал. sender-а
                        NodeList senderNodeList = peretokChildNodeList.item(j).getChildNodes();
                        for (int k = 0; k < senderNodeList.getLength(); k++) {
                            if (senderNodeList.item(k).getNodeName().equals("aiis")) {
                                Area area = new Area();
                                //List<MeasuringPoint> measuringPointList = new ArrayList<>();
                                //List<Node> measPointNodeList = new ArrayList<>();
                                area.setInn(senderNodeList.item(k).getAttributes().getNamedItem("aiiscode").
                                        getNodeValue());
                                area.setName(senderNodeList.item(k).getAttributes().getNamedItem("name").
                                        getNodeValue());
                                Node aiisNode = senderNodeList.item(k);
                                for (int l = 0; l < aiisNode.getChildNodes().getLength(); l++) {
                                    // получаем элемент measPointList-а по узлу measuringpoint
                                    MeasuringPoint measuringPoint = readMeasurePoint(aiisNode.getChildNodes().item(l));
                                    // добавляем этот элемент в список measPointList объекта area (aiis)
                                    //measuringPointList.add(measuringPoint);
                                    area.addMeasPoint(measuringPoint);
                                    // добавляем этот узел measuringpoint в список measPointNodeList объекта area
                                    //measPointNodeList.add(aiisNode.getChildNodes().item(l));
                                }
                                //area.setMeasPointList(measuringPointList);
                                //area.setMeasPointNodeList(measPointNodeList);
                                this.addArea(area);
                            }
                        }
                        break;
                    }
                }
                break; // выходим из цикла, если peretok найден
            }
        }

    }

    @Override
    public void readSender() {

    }

    // загружаем данные из текущего xml-файла
    /*
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
    }*/

}
