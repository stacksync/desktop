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
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneChunk.CacheStatus;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
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
public class NewIndexRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(NewIndexRequest.class.getName());
    
    private File file;
    private CloneFile previousVersion;    
    private long checksum;

    public NewIndexRequest(Folder root, File file, CloneFile previousVersion, long checksum) {
        super(root);
  
        this.file = file;        
        this.previousVersion = previousVersion;
        this.checksum = checksum;
    }

    @Override
    public void process() {                
        logger.info("Indexer: Indexing new file "+file+" ...");
        
        // ignore file        
        if (FileUtil.checkIgnoreFile(root, file)) {
            logger.info("#ndexer: Ignore file "+file+" ...");
            return;
        }
        
        // File vanished
        if (!file.exists()) {
            logger.warn("Indexer: Error indexing file "+file+": File does NOT exist. Ignoring.");            
            return;
        }
        
        // Find file in DB
        CloneFile dbFile = db.getFileOrFolder(root, file);
        if(dbFile != null){
            if (previousVersion == null){
                logger.warn("Indexer: Error already NewIndexRequest proccessed. Ignoring.");
                return;
            } else {
                if(dbFile.getChecksum() == checksum){
                    logger.warn("Indexer: Error already Indexed this version. Ignoring.");
                    return;
                }
            }
            
            if(dbFile.isFolder()){
                logger.warn("Indexer: Error already NewIndexRequest this folder. Ignoring.");
                return;
            }
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        
        // Create DB entry
        CloneFile newVersion = (previousVersion == null) ? addNewVersion() : addChangedVersion();                      

        File parentFile = FileUtil.getCanonicalFile(file.getParentFile());
        newVersion.setParent(db.getFolder(root, parentFile));
        
        // This will check if the file is inside a folder that isn't created.
        if (newVersion.getParent() == null && !newVersion.getPath().equals("/")) {
            Indexer.getInstance().queueNewIndex(root, file, previousVersion, checksum);
            return;
        }
        
        newVersion.setFolder(file.isDirectory());
        newVersion.setFileSize(file.length());
        
        newVersion.setLastModified(new Date(file.lastModified()));
        newVersion.setClientName(config.getMachineName());
        newVersion.setUpdated(new Date());
        newVersion.setSyncStatus(SyncStatus.LOCAL);
        newVersion.merge();
        
        // Process folder and files differently
        if (file.isDirectory()) {
            processFolder(newVersion);
        } else if (file.isFile()) {
            processFile(newVersion);
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }

    private CloneFile addNewVersion() {
        CloneFile newVersion = new CloneFile(root, file);        
                
        newVersion.setVersion(1);
        newVersion.setStatus(Status.NEW);        
        
        return newVersion;
    }
    
    private CloneFile addChangedVersion() {        
        CloneFile newVersion = (CloneFile) previousVersion.clone();
        
        if (newVersion.getSyncStatus() == SyncStatus.UNSYNC
                && previousVersion.getStatus() != Status.RENAMED) {
            if (previousVersion.getVersion() == 1) {
                previousVersion.remove();
                newVersion = this.addNewVersion();
            } else {
                newVersion.setStatus(Status.CHANGED);
                newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
                newVersion.setMimetype(FileUtil.getMimeType(newVersion.getFile()));
                previousVersion.remove();
            }
        } else {
            newVersion.setVersion(previousVersion.getVersion()+1);
            newVersion.setStatus(Status.CHANGED);
            newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
            newVersion.setMimetype(FileUtil.getMimeType(newVersion.getFile()));
        }

        return newVersion;
    }
    
    private void processFolder(CloneFile cf) {        
        // Add rest of the DB stuff 
        cf.setChecksum(0);
        
        if (FileUtil.checkIllegalName(cf.getName())
                || FileUtil.checkIllegalName(cf.getPath().replace(File.separator, ""))){
            cf.setSyncStatus(SyncStatus.UNSYNC);
        } else {
            cf.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        }
        cf.merge();
        
        // Analyze file tree (if directory) -- RECURSIVELY!!
        logger.info("Indexer: Indexing CHILDREN OF "+file+" ...");
        for (File child: file.listFiles()) {
            // Ignore .ignore files
            if (FileUtil.checkIgnoreFile(root, child)) {
                continue; 
            }
            
            // Do it!
            logger.info("Indexer: Parent: "+file+" / CHILD "+child+" ...");
            //new NewIndexRequest(root, child, null).process();
            Indexer.getInstance().queueNewIndex(root, child, null, -1);
        }
    }

    private void processFile(CloneFile cf) {
        try {
            // 1. Chunk it!
            FileChunk chunkInfo = null;
                        
            Folder folderProfile = cf.getProfile().getFolders().get(cf.getRootId()); 
            String path = file.getParent().replace(folderProfile.getLocalFile().getPath(), "");
            path = path.replace('\\', '/');

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);
            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();                
                int chunkOrder = Integer.parseInt(Long.toString(chunkInfo.getNumber()));

                // create chunk in DB (or retrieve it)
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), path, chunkOrder, CacheStatus.CACHED);                         
                
                // write encrypted chunk (if it does not exist)
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);

                byte[] packed = FileUtil.pack(chunkInfo.getContents(), root.getProfile().getRepository().getEncryption());
                if (!chunkCacheFile.exists()) {
                    FileUtil.writeFile(packed, chunkCacheFile);
                } else{
                    if(chunkCacheFile.length() != packed.length){
                        FileUtil.writeFile(packed, chunkCacheFile);
                    }
                }
                
                cf.addChunk(chunk);
            }      
            
            logger.info("Indexer: saving chunks...");
            cf.merge();
            logger.info("Indexer: chunks saved...");
            
            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) { // The last chunk holds the file checksum                
                cf.setChecksum(chunkInfo.getFileChecksum()); 
            } else {
                cf.setChecksum(checksum);
            }
            cf.merge();
            chunks.closeStream();
            
            // 3. Upload it
            if (FileUtil.checkIllegalName(cf.getName())
                    || FileUtil.checkIllegalName(cf.getPath().replace(File.separator, ""))){
                logger.info("This filename contains illegal characters.");
                cf.setSyncStatus(SyncStatus.UNSYNC);
                cf.merge();
            } else {
                logger.info("Indexer: Added to DB. Now Q file "+file+" at uploader ...");
                root.getProfile().getUploader().queue(cf);
            }
            
        } catch (Exception ex) {
            logger.error("Could not index new file "+file+". IGNORING.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        } 
    }
    
    @Override
    public String toString() {
        return NewIndexRequest.class.getSimpleName() + "[" + "file=" + file + "]";
    }    
}
