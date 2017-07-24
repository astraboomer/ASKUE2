package Classes;

public class Period30Min {
    private String start;
    private String end;
    private String status;
    private String extendedstatus;
    private String param1;
    private int value;

    public Period30Min() {
        this.status = "0";
    }

    public String getStart() {
        return start;
    }

    public String getExtendedstatus() {
        return extendedstatus;
    }

    public void setExtendedstatus(String extendedstatus) {
        this.extendedstatus = extendedstatus;
    }

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
