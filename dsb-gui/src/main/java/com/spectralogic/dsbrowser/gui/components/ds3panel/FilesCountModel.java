package com.spectralogic.dsbrowser.gui.components.ds3panel;

public class FilesCountModel {

    private int noOfFolders = 0;
    private int noOfFiles = 0;
    private long totalCapacity = 0;

    public int getNoOfFolders() {
        return noOfFolders;
    }

    public void setNoOfFolders(final int noOfFolders) {
        this.noOfFolders = noOfFolders;
    }

    public int getNoOfFiles() {
        return noOfFiles;
    }

    public void setNoOfFiles(final int noOfFiles) {
        this.noOfFiles = noOfFiles;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(final long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
