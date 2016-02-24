package com.spectralogic.dsbrowser.gui.util;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet;

import java.util.Optional;

public class PropertyItem implements PropertySheet.Item {

    private final String name;
    private final Property property;
    private final String category;
    private final String description;
    private final Class<?> type;

    public PropertyItem(final String name, final Property property, final String category, final String description, final Class<?> type) {
        this.name = name;
        this.property = property;
        this.category = category;
        this.description = description;
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Object getValue() {
        return property.getValue();
    }

    @Override
    public void setValue(final Object value) {
        property.setValue(value);
    }

    @Override
    public Optional<ObservableValue<? extends Object>> getObservableValue() {
        return Optional.of(property);
    }
}
