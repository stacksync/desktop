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
import java.io.FileNotFoundException;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneItemVersion.SyncStatus;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class CheckIndexRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(CheckIndexRequest.class.getName());
    
    private File file;

    public CheckIndexRequest(Folder root, File file) {
        super(root);    
        this.file = file;
    }

    @Override
    public void process() {
        logger.debug("Indexer: Checking file "+file.getAbsoluteFile()+" ... ");
        
        // ignore file
        if (FileUtil.checkIgnoreFile(root, file)) {
            logger.info("Indexer: Ignore file "+file+" ...");
            return;
        }
                        
        // Ignore if non-existant
        if (!file.exists()) {            
            logger.warn("Indexer: File does not exist: "+file.getAbsolutePath()+". IGNORING.");            
            return;
        }

        // Folder
        if (file.isDirectory()) {
            processFolder();
        } else if (file.isFile()) { // File
            processFile();            
        }
        logger.debug("Checking file DONE: "+file.getAbsoluteFile()+" ... ");  
    }

    private void processFolder() {
        CloneItem dbFile = db.getFolder(root, file);

        // Matching DB entry found
        if (dbFile != null) {
            logger.debug("Folder " + dbFile.getFile().toString() + " FOUND in DB. Nothing to do.");
        } else {           
            // Add as new
            logger.info("Folder " + file.toString() + " NOT found in DB. Adding as new file.");
            Indexer.getInstance().queueNewIndex(root, file, null, -1);
        }
    }

    private void processFile() {
        // Find file in DB
        CloneItem dbFile = db.getFile(root, file);
        
        // Find checksum of file; 
        long fileCheckSum;

        try {
            // TODO This is inefficient, if the file is 'new', since the NewIndexRequest (below)
            // TODO does create checksums for all the chunks again!
            //fileCheckSum = chunker.createChecksum(file, root.getProfile().getRepository().getChunkSize());                                    
            fileCheckSum = chunker.createFileChecksum(file);
        } catch (FileNotFoundException e) {
            logger.warn("Could not create checksum of "+file+". File not found. IGNORING.", e);          
            return;                
        }        
        
        // Matching DB entry found; Now check filesize and time
        if (dbFile != null) {
            CloneItemVersion dbLatestVersion = dbFile.getLatestVersion();
            if (dbLatestVersion == null) {
                logger.error("File " + dbFile.getName() + "-" + dbFile.getId() + " has no latest version." );
                return;
            }
            
            if(dbLatestVersion.getChecksum() == 0 && dbLatestVersion.getSyncStatus() == SyncStatus.LOCAL){
                logger.debug("File " + dbFile.getName() + " is indexing now. Nothing to do!");    
                return;
            }
            
            boolean isSameFile = Math.abs(file.lastModified() - dbLatestVersion.getLastModified().getTime()) < 500
                && file.length() == dbLatestVersion.getSize();            
            
            if (isSameFile || fileCheckSum == dbLatestVersion.getChecksum()) {
                logger.debug("File " + dbFile.getName() + " found in DB. Same modified date, same size. Nothing to do!");    
                return;
            }
            
            logger.info("File " + dbFile.getName() + " found, but modified date or size differs. Indexing as CHANGED file.");
            logger.info("-> fs = ("+file.lastModified()+", "+file.length()+"), db = ( "+dbLatestVersion.getLastModified().getTime()+", "+dbLatestVersion.getSize()+")");
            
            //new NewIndexRequest(root, file, dbFile).process();
            Indexer.getInstance().queueNewIndex(root, file, dbFile, fileCheckSum);
        
        } else if (dbFile == null) {
            logger.info("No previous version found. Adding new file ...");
            Indexer.getInstance().queueNewIndex(root, file, null, fileCheckSum);
        }  
    }
}
