package com.spectralogic.dsbrowser.gui.util;

import java.io.Serializable;

public class BaseTreeModel implements Serializable {
    protected String name;
    protected Type type;

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        File, Directory, Bucket, Loader, Media_Device, File_System, Error
    }

}
