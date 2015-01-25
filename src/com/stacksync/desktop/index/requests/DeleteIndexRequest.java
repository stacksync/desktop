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
package com.stacksync.desktop.index.requests;

import java.io.File;
import java.util.List;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneItemVersion.Status;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class DeleteIndexRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(DeleteIndexRequest.class.getName());
    
    private CloneItem dbFile;
    private File file;

    public DeleteIndexRequest(Folder root, File file) {
        super(root);

        this.dbFile = null;
        this.file = file;
    }
    
    public DeleteIndexRequest(Folder root, CloneItem dbFile) {
        super(root);
        this.dbFile = dbFile;
        this.file = dbFile.getFile();
    }    

    public File getFile() {
        return file;
    }

    @Override
    public void process() {
        logger.info("Indexer: Deleting file  "+file.getAbsolutePath());
        
        // ignore file
        if (FileUtil.checkIgnoreFile(root, file)) {
            return;
        }
                     
        // Find last version of this file in DB
        if (dbFile == null) {
            dbFile = db.getFileOrFolder(root, file);
        }

        // Not found?!
        if (dbFile == null) {
            logger.warn("Indexer: File not found in DB ("+file.getAbsolutePath()+"). IGNORING.");
            return;
        }
        
        // File found in DB.
        CloneItemVersion latestVersion = dbFile.getLatestVersion();
        
        if (latestVersion.getSyncStatus() == CloneItemVersion.SyncStatus.UNSYNC) {

            // TODO, FIXME, IMPORTANT Adapt this code to work with item versions.
            CloneItemVersion lastSynced = dbFile.getLastSyncedVersion();

            if (lastSynced == null) {
                // No exist a legal version synchronized -> remove item!!
                // NOT TESTED
                dbFile.remove();
            } else {
                // Use next version and forget about the UNSYNC versions.
                latestVersion = (CloneItemVersion) lastSynced.clone();
                latestVersion.setVersion(lastSynced.getVersion()+1);
                // Updated changes
                latestVersion.setStatus(Status.DELETED);
                latestVersion.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);

                // Before merge this new version, I have to remove unsync versions
                // to avoid duplicated PK in database!
                dbFile.addVersion(latestVersion);
                latestVersion.merge();
            }
                
        } else {

            // Updated changes
            CloneItemVersion deletedVersion = (CloneItemVersion) latestVersion.clone();
            deletedVersion.setVersion(latestVersion.getVersion()+1);
            deletedVersion.setStatus(Status.DELETED);
            deletedVersion.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
            dbFile.addVersion(deletedVersion);

            deletedVersion.merge();
        }
        
        // Notify file manager (invalidate!)
        desktop.touch(file);        

        // Delete children (if directory) -- RECURSIVELY !!
        if (dbFile.isFolder()) {     
            logger.info("Indexer: Deleting CHILDREN of "+file+" ...");
            List<CloneItem> children = db.getNotDeletedChildren(dbFile);

            for (CloneItem child : children) {
                logger.info("Indexer: Delete CHILD "+child.getAbsolutePath()+" ...");
                // Do it!
                Indexer.getInstance().queueDeleted(root, child);
            }
        }

    }
}