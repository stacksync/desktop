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
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneChunk.CacheStatus;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion.Status;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class MoveIndexRequest extends IndexRequest {
    
    private final Logger logger = Logger.getLogger(MoveIndexRequest.class.getName());
    
    private CloneItem dbFromFile;
    
    private Folder fromRoot;
    private File fromFile;
    
    private Folder toRoot;
    private File toFile;    

    public MoveIndexRequest(Folder fromRoot, File fromFile, Folder toRoot, File toFile) {
        super();
         
        this.fromRoot = fromRoot;
        this.fromFile = fromFile;
        
        this.toRoot = toRoot;
        this.toFile = toFile;
        
    }
    
    public MoveIndexRequest(CloneItem dbFromFile, Folder toRoot, File toFile) {
        this(dbFromFile.getRoot(), dbFromFile.getFile(), toRoot, toFile);
        this.dbFromFile = dbFromFile;
    }
    
    @Override
    public void process() {
        logger.info("Indexer: Updating moved file "+fromFile.getAbsolutePath()+" TO "+toFile.getAbsolutePath()+"");
        
        // ignore file
        if (FileUtil.checkIgnoreFile(fromRoot, fromFile) || FileUtil.checkIgnoreFile(toRoot, toFile)) {
            logger.info("Indexer: Ignore file " + fromFile + " or toFile " + toFile + " ...");
            return;
        }
        
        // File vanished
        if (!toFile.exists()) {
            logger.warn("Indexer: Error indexing fromfile " + fromFile + " or toFile " + toFile + ": Files does NOT exist. Ignoring.");            
            return;
        }
                
        // Look for file in DB
        if (dbFromFile == null) {
            dbFromFile = db.getFileOrFolder(fromRoot, fromFile);
            
            // No file found in DB.
            if (dbFromFile == null) {
                logger.warn("Indexer: Source file not found in DB ("+fromFile.getAbsolutePath()+"). Indexing "+toFile.getAbsolutePath()+" as new file.");
                
                Indexer.getInstance().queueChecked(toRoot, toFile);
                return;
            }            
        }
        
        if (dbFromFile.isWorkspaceRoot()) {
            // Apply special process
            
            if (!fromFile.getName().equals(toFile.getName())) { // Check rename
                Indexer.getInstance().queueRenamedWorkspace(dbFromFile, toFile);
            } else { // Check move
                Indexer.getInstance().queueMovedWorkspace(dbFromFile, toRoot, toFile);
            }
            return;
        }

        // Parent 
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        CloneItem cToParentFolder = db.getFolder(toRoot, new File(absToParentFolder));
        
        // Check if the movement is between different workspaces
        CloneItem fromFileParent = dbFromFile.getParent();
        CloneWorkspace fromWorkspace = (fromFileParent == null) ? dbFromFile.getWorkspace() : fromFileParent.getWorkspace();
        CloneWorkspace toWorkspace = (cToParentFolder == null) ? db.getDefaultWorkspace() : cToParentFolder.getWorkspace();
        if (isWorkspaceChanged(fromWorkspace, toWorkspace)) {
            logger.info("Item workspace changed. Queueing delete and new requests.");
            Indexer.getInstance().queueDeleted(fromRoot, fromFile);
            Indexer.getInstance().queueNewIndex(toRoot, toFile, null, -1);
            return;
        }
                
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);

        // File found in DB.
        CloneItemVersion latestVersion = dbFromFile.getLatestVersion();
        CloneItemVersion newVersion = (CloneItemVersion) latestVersion.clone();

        // Updated changes
        dbFromFile.setRoot(toRoot);
        newVersion.setLastModified(new Date(toFile.lastModified()));
        dbFromFile.setName(toFile.getName());
        newVersion.setSize((toFile.isDirectory()) ? 0 : toFile.length());
        newVersion.setVersion(latestVersion.getVersion()+1);
        newVersion.setStatus(Status.RENAMED);
        newVersion.setSyncStatus(CloneItemVersion.SyncStatus.LOCAL);

        dbFromFile.setParent(cToParentFolder);
        dbFromFile.generatePath();
        dbFromFile.setLatestVersionNumber(newVersion.getVersion());
        dbFromFile.merge();
	    
        // Notify file manager
        this.desktop.touch(dbFromFile.getFile());
        this.desktop.touch(dbFromFile.getAbsolutePath(), CloneItemVersion.SyncStatus.SYNCING);
	    
        if (!dbFromFile.isFolder()) {
            processFile(newVersion);
        } else {
            processFolder(newVersion);
        }  
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
    
    private void processFile(CloneItemVersion itemVersion) {
        try {
            File file = dbFromFile.getFile();
            Folder root = dbFromFile.getRoot();
            
            // 1. Chunk it!
            FileChunk chunkInfo = null;

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);

            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();
                int order = Integer.parseInt(Long.toString(chunkInfo.getNumber()));
                
                // create chunk in DB (or retrieve it)
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), CacheStatus.CACHED);                         
                
                // write chunk (if it does not exist)
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);

                if (!chunkCacheFile.exists()) {
                    byte[] packed = FileUtil.pack(chunkInfo.getContents(), root.getProfile().getEncryption(dbFromFile.getWorkspace().getId()));                    
                    FileUtil.writeFile(packed, chunkCacheFile);                   
                }
                
                if(itemVersion.getChunks().isEmpty() || chunkInfo.getNumber() > itemVersion.getChunks().size()){
                    itemVersion.addChunk(chunk);
                }

                CloneChunk chunkOriginal = itemVersion.getChunks().get(order);
                if(chunkInfo.getChecksum().compareTo(chunkOriginal.getChecksum()) != 0){
                    itemVersion.getChunks().set(order, chunk);
                }                
            }
            
            
            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) {
                // The last chunk holds the file checksum
                itemVersion.setChecksum(chunkInfo.getFileChecksum()); 
            }
            chunks.closeStream();            
            itemVersion.merge();
            
            // 3. CHECKS SECTION
            // 3a. Check if file name contains specials Windows characters (:"\{...)
            if (FileUtil.checkIllegalName(dbFromFile.getName())
                    || FileUtil.checkIllegalName(dbFromFile.getPath().replace("/", ""))){
                logger.info("This filename contains illegal characters.");
                itemVersion.setSyncStatus(CloneItemVersion.SyncStatus.UNSYNC);
                itemVersion.merge();
                return;
            }
            
            // 3b. TODO Check storage free space
            
            // 4. If previous version was UNSYNC
            /*if (dbFromFile.getSyncStatus() == CloneItemVersion.SyncStatus.UNSYNC){
                
                // Search for the last synced version to create the next version
                CloneItem lastSyncedVersion = itemVersion.getLastSyncedVersion();
                if (lastSyncedVersion == null) {
                    itemVersion.setVersion(1);
                    itemVersion.setStatus(Status.NEW);
                } else {
                    itemVersion.setVersion(lastSyncedVersion.getVersion()+1);
                }
                
                itemVersion.merge();
                
                // Clean unsynced versions from DB
                itemVersion.deleteHigherVersion();
            }*/
            
            // 5. Upload it
            logger.info("Indexer: Added to DB. Now Q file "+file+" at uploader ...");
            root.getProfile().getUploader().queue(itemVersion);

        } catch (Exception ex) {
            logger.error("Could not index new file. IGNORING.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
    }
    
    private void processFolder(CloneItemVersion itemVersion) {
        
        // 4. If previous version was UNSYNC
        /*if (dbFromFile.getSyncStatus() == CloneItem.SyncStatus.UNSYNC){

            // Search for the last synced version to create the next version
            CloneItem lastSyncedVersion = itemVersion.getLastSyncedVersion();
            if (lastSyncedVersion == null) {
                itemVersion.setVersion(1);
                itemVersion.setStatus(Status.NEW);
            } else {
                itemVersion.setVersion(lastSyncedVersion.getVersion()+1);
            }

            itemVersion.merge();

            // Clean unsynced versions from DB
            itemVersion.deleteHigherVersion();
        }*/
        
        
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        
        // Update children (if directory) -- RECURSIVELY !!
        logger.info("Indexer: Updating CHILDREN of "+toFile+" ...");   
        List<CloneItem> children = db.getNotDeletedChildren(dbFromFile);

        for (CloneItem child : children) {
            File childFromFile = child.getFile();
            File childToFile = new File(absToParentFolder+File.separator+toFile.getName()+File.separator+child.getName());
            logger.info("Indexer: Updating children of moved file "+childFromFile.getAbsolutePath()+" TO "+childToFile.getAbsolutePath()+"");

            // Do it!
            new MoveIndexRequest(fromRoot, childFromFile, toRoot, childToFile).process();
        }
        
        if (FileUtil.checkIllegalName(dbFromFile.getName())
                || FileUtil.checkIllegalName(dbFromFile.getPath().replace("/", ""))){
            logger.info("This folder contains illegal characters.");
            itemVersion.setSyncStatus(CloneItemVersion.SyncStatus.UNSYNC);
            itemVersion.merge();
            return;
        }
        
        itemVersion.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        itemVersion.merge();
        
    }
    
    private boolean isWorkspaceChanged(CloneWorkspace fromWorkspace, CloneWorkspace toWorkspace) {
        
        boolean workspaceChanged = true;
        
        if (fromWorkspace.getId().equals(toWorkspace.getId())) {
            workspaceChanged = false;
        }
        
        return workspaceChanged;
    }
}