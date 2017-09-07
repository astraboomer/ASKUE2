package Classes;

public class MeasuringData {
    private String timeInterval;
    private String value;
    private String typeInfo;

    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(String typeInfo) {
        this.typeInfo = typeInfo;
    }

    public MeasuringData(String time, String value, String typeInfo) {
        this.timeInterval = time;
        if (typeInfo != null)
            this.typeInfo = typeInfo;
        else
            this.typeInfo = "0";
        this.value = value;
    }

}
