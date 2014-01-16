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
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class DeleteIndexRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(DeleteIndexRequest.class.getName());
    
    private CloneFile dbFile;
    private File file;

    public DeleteIndexRequest(Folder root, File file) {
        super(root);

        this.dbFile = null;
        this.file = file;
    }
    
    public DeleteIndexRequest(Folder root, CloneFile dbFile) {
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
        CloneFile deletedVersion = (CloneFile) dbFile.clone();
        
        if (deletedVersion.getSyncStatus() == CloneFile.SyncStatus.UNSYNC) {

            CloneFile lastSynced = deletedVersion.getLastSyncedVersion();

            if (lastSynced == null) {
                // No exist a legal version synchronized -> Delete all!!
                deletedVersion.setVersion(0);
            } else {
                // Use next version and forget about the UNSYNC versions.
                deletedVersion = (CloneFile) lastSynced.clone();
                deletedVersion.setVersion(lastSynced.getVersion()+1);

                // Updated changes
                deletedVersion.setUpdated(new Date());
                deletedVersion.setStatus(Status.DELETED);
                deletedVersion.setSyncStatus(CloneFile.SyncStatus.UPTODATE);

                deletedVersion.merge();
                
            }
            
            deletedVersion.deleteHigherVersion();
                
        } else {

            // Updated changes
            deletedVersion.setVersion(deletedVersion.getVersion()+1);
            deletedVersion.setUpdated(new Date());
            deletedVersion.setStatus(Status.DELETED);
            //deletedVersion.setSyncStatus(CloneFile.SyncStatus.LOCAL);
            deletedVersion.setSyncStatus(CloneFile.SyncStatus.UPTODATE);

            deletedVersion.merge();
        }
        
        // Notify file manager (invalidate!)
        desktop.touch(file);        

        // Delete children (if directory) -- RECURSIVELY !!
        if (dbFile.isFolder()) {     
            logger.info("Indexer: Deleting CHILDREN of "+file+" ...");
            List<CloneFile> children = db.getChildren(dbFile);

            for (CloneFile child : children) {
                logger.info("Indexer: Delete CHILD "+child.getAbsolutePath()+" ...");
                // Do it!
                //new DeleteIndexRequest(root, child).process();
                Indexer.getInstance().queueDeleted(root, child);
            }
        }

    }
}