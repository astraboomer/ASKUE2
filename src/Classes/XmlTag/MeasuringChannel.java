package Classes.XmlTag;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import org.w3c.dom.NodeList;

import java.awt.*;

/**
 * Created by Сергей on 10.05.2017.
 */
//
public class MeasuringChannel extends CheckBox {
    private String code;
    private boolean isCommercialInfo = true;
    private BooleanProperty selected = new SimpleBooleanProperty(true);
    private ReadOnlyStringWrapper desc = new ReadOnlyStringWrapper();
    private String aliasName;
    private ContextMenu contextMenu = new ContextMenu();
    private MenuItem unCommMenuItem = new MenuItem("Пометить как некоммер.");
    private MenuItem showDataItem = new MenuItem("Показать данные");

    public MenuItem getShowDataItem() {
        return showDataItem;
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

}
