package Classes;

import Classes.XmlTag.MeasuringPoint;
import org.w3c.dom.Node;

import java.util.List;

public class AIIS {
    private String name;
    private String code;
    private List<MeasuringPoint> measPointList;
    private List<Node> measPointNodeList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<MeasuringPoint> getMeasPointList() {
        return measPointList;
    }

    public void setMeasPointList(List<MeasuringPoint> measPointList) {
        this.measPointList = measPointList;
    }

    public List<Node> getMeasPointNodeList() {
        return measPointNodeList;
    }

    public void setMeasPointNodeList(List<Node> measPointNodeList) {
        this.measPointNodeList = measPointNodeList;
    }
}
