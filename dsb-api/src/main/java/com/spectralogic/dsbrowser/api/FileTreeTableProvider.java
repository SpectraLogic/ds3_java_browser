package com.spectralogic.dsbrowser.api;

import com.google.common.collect.ImmutableList;

import java.io.IOException;

public interface FileTreeTableProvider {
    ImmutableList<FileTreeModel> getRoot();
    ImmutableList<FileTreeModel> getListForDir(final String dirName) throws IOException;
}
