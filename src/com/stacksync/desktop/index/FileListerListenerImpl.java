/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.index;

import java.io.File;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.util.FileLister.FileListerListener;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class FileListerListenerImpl implements FileListerListener {
    private Folder root;
    private boolean deleteIgnoreFiles;

    public FileListerListenerImpl(Folder root, Indexer indexer, boolean deleteIgnoreFiles) {
        this.root = root;
        //this.indexer = indexer;
        this.deleteIgnoreFiles = deleteIgnoreFiles;
    }

    @Override
    public void proceedFile(File file) {
        //System.err.println(file.getAbsoluteFile());
        //new CheckIndexRequest(root, file).process();
        Indexer.getInstance().queueChecked(root, file);
    }

    @Override
    public void enterDirectory(File directory) {
        //new CheckIndexRequest(root, directory).process();
        Indexer.getInstance().queueChecked(root, directory);
    }

    @Override
    public boolean directoryFilter(File directory) {
        if (FileUtil.checkIgnoreFile(root, directory)) {
            if (deleteIgnoreFiles && FileUtil.checkStackSyncTemporalFile(root, directory)) {
                FileUtil.deleteRecursively(directory);
            }

            return false;
        }

        return true;
    }

    @Override
    public boolean fileFilter(File file) {
        if (FileUtil.checkIgnoreFile(root, file)) {
            if (deleteIgnoreFiles && FileUtil.checkStackSyncTemporalFile(root, file)) {
                FileUtil.deleteRecursively(file);
            }

            return false;
        }

        return true;
    }

    @Override public void outDirectory(File directory) { }
    @Override public void startProcessing() { }
    @Override public void endOfProcessing() { }
}