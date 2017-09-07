package Classes.XmlTag;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Сергей on 10.05.2017.
 */
public class Area {
    private String inn;
    private String name;
    private String timeZone;
    private List<MeasuringPoint> measPointList = new ArrayList<>();
    //private List<Node> measPointNodeList;

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    /*
    public List<Node> getMeasPointNodeList() {
        return measPointNodeList;
    }

    public void setMeasPointNodeList(List<Node> measPointNodeList) {
        this.measPointNodeList = measPointNodeList;
    }*/

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MeasuringPoint> getMeasPointList() {
        return measPointList;
    }

    /*public void setMeasPointList(List<MeasuringPoint> measPointList) {
        this.measPointList = measPointList;
    } */
    public void addMeasPoint(MeasuringPoint measPoint) {
        measPointList.add(measPoint);
    }
}
