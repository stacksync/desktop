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
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class MoveIndexRequest extends IndexRequest {
    
    private final Logger logger = Logger.getLogger(MoveIndexRequest.class.getName());
    
    private CloneFile dbFromFile;
    
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
    
    public MoveIndexRequest(CloneFile dbFromFile, Folder toRoot, File toFile) {
        this(dbFromFile.getRoot(), dbFromFile.getFile(), toRoot, toFile);
        this.dbFromFile = dbFromFile;
    }
    
    /*
     * TODO moving a file from one ROOT to another does not work (cp. database!)
     * 
     */
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
                
                //new CheckIndexRequest(toRoot, toFile).process();
                Indexer.getInstance().queueChecked(toRoot, toFile);
                return;
            }            
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);

        // Parent 
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        CloneFile cToParentFolder = db.getFolder(toRoot, new File(absToParentFolder));

        // File found in DB.
        CloneFile dbToFile = (CloneFile) dbFromFile.clone();

        // Updated changes
        dbToFile.setRoot(toRoot);
        dbToFile.setLastModified(new Date(toFile.lastModified()));
        dbToFile.setName(toFile.getName());
        dbToFile.setSize((toFile.isDirectory()) ? 0 : toFile.length());
        dbToFile.setVersion(dbToFile.getVersion()+1);
        dbToFile.setStatus(Status.RENAMED);
        dbToFile.setSyncStatus(CloneFile.SyncStatus.LOCAL);

        dbToFile.setParent(cToParentFolder);
        dbToFile.setMimetype(FileUtil.getMimeType(dbToFile.getFile()));
        dbToFile.generatePath();
        dbToFile.merge();
	    
        // Notify file manager
        desktop.touch(dbToFile.getFile());
	    
        if (!dbToFile.isFolder()) {
            processFile(dbToFile);
        } else {
            processFolder(dbToFile);
        }  
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
    
    private void processFile(CloneFile cf) {
        try {
            File file = cf.getFile();
            Folder root = cf.getRoot();
            
            // 1. Chunk it!
            FileChunk chunkInfo = null;

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);

            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();
                int order = Integer.parseInt(Long.toString(chunkInfo.getNumber()));
                
                // create chunk in DB (or retrieve it)
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), CacheStatus.CACHED);                         
                
                // write encrypted chunk (if it does not exist)
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);

                if (!chunkCacheFile.exists()) {
                    byte[] packed = FileUtil.pack(chunkInfo.getContents(), root.getProfile().getRepository().getEncryption());                    
                    FileUtil.writeFile(packed, chunkCacheFile);                   
                }
                
                if(cf.getChunks().isEmpty() || chunkInfo.getNumber() > cf.getChunks().size()){
                    cf.addChunk(chunk);
                }

                CloneChunk chunkOriginal = cf.getChunks().get(order);
                if(chunkInfo.getChecksum().compareTo(chunkOriginal.getChecksum()) != 0){
                    cf.getChunks().set(order, chunk);
                }                
            }
            
            
            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) {
                // The last chunk holds the file checksum
                cf.setChecksum(chunkInfo.getFileChecksum()); 
            }
            chunks.closeStream();            
            cf.merge();
            
            // 3. CHECKS SECTION
            // 3a. Check if file name contains specials Windows characters (:"\{...)
            if (FileUtil.checkIllegalName(cf.getName())
                    || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))){
                logger.info("This filename contains illegal characters.");
                cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
                cf.merge();
                return;
            }
            
            // 3b. TODO Check storage free space
            
            // 4. If previous version was UNSYNC
            if (dbFromFile.getSyncStatus() == CloneFile.SyncStatus.UNSYNC){
                
                // Search for the last synced version to create the next version
                CloneFile lastSyncedVersion = cf.getLastSyncedVersion();
                if (lastSyncedVersion == null) {
                    cf.setVersion(1);
                    cf.setStatus(Status.NEW);
                } else {
                    cf.setVersion(lastSyncedVersion.getVersion()+1);
                }
                
                cf.merge();
                
                // Clean unsynced versions from DB
                cf.deleteHigherVersion();
            }
            
            // 5. Upload it
            logger.info("Indexer: Added to DB. Now Q file "+file+" at uploader ...");
            root.getProfile().getUploader().queue(cf);

        } catch (Exception ex) {
            logger.error("Could not index new file. IGNORING.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
    }
    
    private void processFolder(CloneFile cf) {
        
        // 4. If previous version was UNSYNC
        if (dbFromFile.getSyncStatus() == CloneFile.SyncStatus.UNSYNC){

            // Search for the last synced version to create the next version
            CloneFile lastSyncedVersion = cf.getLastSyncedVersion();
            if (lastSyncedVersion == null) {
                cf.setVersion(1);
                cf.setStatus(Status.NEW);
            } else {
                cf.setVersion(lastSyncedVersion.getVersion()+1);
            }

            cf.merge();

            // Clean unsynced versions from DB
            cf.deleteHigherVersion();
        }
        
        
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        
        // Update children (if directory) -- RECURSIVELY !!
        logger.info("Indexer: Updating CHILDREN of "+toFile+" ...");   
        List<CloneFile> children = db.getChildren(dbFromFile);

        for (CloneFile child : children) {
            File childFromFile = child.getFile();
            File childToFile = new File(absToParentFolder+File.separator+toFile.getName()+File.separator+child.getName());
            logger.info("Indexer: Updating children of moved file "+childFromFile.getAbsolutePath()+" TO "+childToFile.getAbsolutePath()+"");

            // Do it!
            new MoveIndexRequest(fromRoot, childFromFile, toRoot, childToFile).process();
        }
        
        if (FileUtil.checkIllegalName(cf.getName())
                || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))){
            logger.info("This folder contains illegal characters.");
            cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
            cf.merge();
            return;
        }
        
        cf.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        cf.merge();
        
    }
}