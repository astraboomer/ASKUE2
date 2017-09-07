package Classes.XmlTag;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Сергей on 10.05.2017.
 */
//
public class MeasuringChannel extends CheckBox {
    public static final String ACTIVE_INPUT = "Активная энергия, прием";
    public static final String ACTIVE_OUTPUT = "Активная энергия, отдача";
    public static final String REACTIVE_INPUT = "Реактивная энергия, прием";
    public static final String REACTIVE_OUTPUT = "Реактивная энергия, отдача";

    private String code;
    private boolean isCommercialInfo = true;
    private BooleanProperty selected = new SimpleBooleanProperty(true);
    private ReadOnlyStringWrapper desc = new ReadOnlyStringWrapper();
    private String aliasName;
    private List<Period> periodList = new ArrayList<>();
    private ContextMenu contextMenu = new ContextMenu();
    private MenuItem unCommMenuItem = new MenuItem("Пометить как некоммер.");
    private MenuItem showDataItem = new MenuItem("Показать данные");

    public MenuItem getShowDataItem() {
        return showDataItem;
    }

    public List<Period> getPeriodList() {
        return periodList;
    }

    public void addPeriod (Period period) {
        periodList.add(period);
    }

    public MenuItem getUnCommMenuItem() {
        return unCommMenuItem;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public MeasuringChannel () {
        this.setSelected(true);
        // каждому measuringChannel-у добавляем контекстное меню
        contextMenu.getItems().addAll(showDataItem, unCommMenuItem);
        this.setOnContextMenuRequested(event -> contextMenu.show(this, event.getScreenX(), event.getScreenY()));
    }
    public boolean isCommercialInfo() {
        return isCommercialInfo;
    }
    public void setCommercialInfo(boolean commercialInfo) {
        isCommercialInfo = commercialInfo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc.get();
    }

    public void setDesc(String desc) {
        this.desc.set(desc);
    }

    // получаем название канала по его последней цифре кода
    public String getAliasNameChannelByCode (String code) {
        char ch = code.charAt(1);
        String result = "Неизвестный канал";
        switch (ch) {
            case '1' : result = ACTIVE_INPUT;
                break;
            case '2' : result = ACTIVE_OUTPUT;
                break;
            case '3' : result = REACTIVE_INPUT;
                break;
            case '4' : result = REACTIVE_OUTPUT;
                break;
        }
        return result;
    }

}
