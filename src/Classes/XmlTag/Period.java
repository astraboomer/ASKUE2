package Classes.XmlTag;

public class Period {
    private String start;
    private String end;
    private String interval;
    private String value;
    private String status;
    private String extendedStatus;
    private String param1;

    public String getInterval() {
        return interval;
    }

    public void setInterval() {
        if (start != null && end != null) {
            interval = start.substring(0, 2) + ":" + start.substring(2, 4) +
                    " - " + end.substring(0, 2) + ":" + end.substring(2, 4);
        }
        else
            interval = null;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String getValue() {
        return value;
    }

    public String getStatus() {
        return status;
    }

    public String getExtendedStatus() {
        return extendedStatus;
    }

    public String getParam1() {
        return param1;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setExtendedStatus(String extendedstatus) {
        this.extendedStatus = extendedstatus;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }
}
