package Classes.XmlTag;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;

/**
 * Created by Сергей on 10.05.2017.
 */
public class MeasuringPoint {
    private String code;
    private List<MeasuringChannel> measChannelList;
    private BooleanProperty selected = new SimpleBooleanProperty(true);
    private ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();

    /*public MeasuringPoint () {
        this.setSelected(true);
    }*/

    public BooleanProperty selectedProperty() {
        return selected;
    }
    public boolean isSelected() {
        return selected.get();
    }
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
        //this.setText(name);
    }

    public List<MeasuringChannel> getMeasChannelList() {
        return measChannelList;
    }

    public void setMeasChannelList(List<MeasuringChannel> measChannelList) {
        this.measChannelList = measChannelList;
    }

}
