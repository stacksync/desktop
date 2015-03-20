package com.stacksync.desktop.index.requests;

import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;

public class NewIndexSharedRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(NewIndexRequest.class.getName());
    
    private File file;
    private long checksum;
    private CloneItem previousVersion;

    public NewIndexSharedRequest(Folder root, File file, CloneItem previousVersion, long checksum) {
        super(root);
  
        this.file = file;        
        this.checksum = checksum;
        this.previousVersion = previousVersion;
    }

    @Override
    public void process() {
        logger.info("Indexer: Indexing new share file "+file+" ...");
        
        // Find file in DB
        CloneItem dbFile = db.getFileOrFolder(root, file);
        if(dbFile != null){
            CloneItemVersion oldVersion = dbFile.getLatestVersion();
            if(oldVersion.getChecksum() == checksum){
                logger.warn("Indexer: Error already Indexed this version. Ignoring.");
                return;
            }
            
            if(dbFile.isFolder()){
                logger.warn("Indexer: Error already NewIndexRequest this folder. Ignoring.");
                return;
            }
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        
        // Create DB entry
        CloneItem newItem = (previousVersion == null) ? addNewVersion() : addChangedVersion();

        File parentFile = FileUtil.getCanonicalFile(file.getParentFile());
        CloneItem parentCF = db.getFolder(root, parentFile);
        newItem.setParent(parentCF);
        
        CloneWorkspace workspace = parentCF.getWorkspace();
        newItem.setWorkspace(workspace);
        newItem.setFolder(file.isDirectory());
        
        CloneItemVersion newVersion = newItem.getLatestVersion();
        newVersion.setSize(file.length());
        newVersion.setLastModified(new Date(file.lastModified()));
        newVersion.setSyncStatus(CloneItemVersion.SyncStatus.LOCAL);
        
        newItem.merge();
        
        if (newItem.isFolder())
            processFolder(newItem, newVersion);
        else {
            processFile(newItem, newVersion);
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }

    private CloneItem addNewVersion() {
        CloneItem newItem = new CloneItem(root, file);  
        CloneItemVersion newVersion = new CloneItemVersion(file);
        newVersion.setStatus(CloneItemVersion.Status.NEW);
        newItem.addVersion(newVersion);
        
        return newItem;
    }
    
    private CloneItem addChangedVersion() {        
        
        CloneItemVersion oldVersion = previousVersion.getLatestVersion();

        if (oldVersion.getSyncStatus() == CloneItemVersion.SyncStatus.UNSYNC && oldVersion.getStatus() != CloneItemVersion.Status.RENAMED) {
            if (oldVersion.getVersion() == 1) {
                oldVersion.setServerUploadedAck(false);
                oldVersion.setServerUploadedTime(null);
                oldVersion.setChecksum(0);
                oldVersion.setChunks(new ArrayList<CloneChunk>());
            } else {
                oldVersion.setStatus(CloneItemVersion.Status.CHANGED);
                oldVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
                previousVersion.setMimetype(FileUtil.getMimeType(file));
            }
        } else {
            CloneItemVersion newVersion = (CloneItemVersion) oldVersion.clone();
            newVersion.setVersion(oldVersion.getVersion()+1);
            newVersion.setStatus(CloneItemVersion.Status.CHANGED);
            newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
            previousVersion.setMimetype(FileUtil.getMimeType(previousVersion.getFile()));
            previousVersion.setLatestVersionNumber(newVersion.getVersion());
            previousVersion.addVersion(newVersion);
        }

        return previousVersion;
    }
    
    private void processFolder(CloneItem item, CloneItemVersion version) {        
        // Add rest of the DB stuff 
        version.setChecksum(0);
        
        if (FileUtil.checkIllegalName(item.getName())
                || FileUtil.checkIllegalName(item.getPath().replace("/", ""))){
            version.setSyncStatus(CloneItemVersion.SyncStatus.UNSYNC);
        } else {
            version.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        }
        item.merge();
        
        // Not necessary yet. Shared folders don't contain files.
        // Analyze file tree (if directory) -- RECURSIVELY!!
        /*logger.info("Indexer: Indexing CHILDREN OF "+file+" ...");
        for (File child: file.listFiles()) {
            // Ignore .ignore files
            if (FileUtil.checkIgnoreFile(root, child)) {
                continue; 
            }
            
            // Do it!
            logger.info("Indexer: Parent: "+file+" / CHILD "+child+" ...");
            Indexer.getInstance().queueNewIndex(root, child, null, -1);
        }*/
    }
    
    private void processFile(CloneItem item, CloneItemVersion version) {
        try {
            // 1. Chunk it!
            FileChunk chunkInfo = null;

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);
            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();                

                // create chunk in DB (or retrieve it)
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), CloneChunk.CacheStatus.CACHED);
                
                // write encrypted chunk (if it does not exist)
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);

                byte[] packed = FileUtil.pack(chunkInfo.getContents(), root.getProfile().getEncryption(item.getWorkspace().getId()));
                if (!chunkCacheFile.exists()) {
                    FileUtil.writeFile(packed, chunkCacheFile);
                } else{
                    if(chunkCacheFile.length() != packed.length){
                        FileUtil.writeFile(packed, chunkCacheFile);
                    }
                }
                
                version.addChunk(chunk);
            }      
            
            logger.info("Indexer: saving chunks...");
            item.merge();
            logger.info("Indexer: chunks saved...");
            
            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) { // The last chunk holds the file checksum                
                version.setChecksum(chunkInfo.getFileChecksum()); 
            } else {
                version.setChecksum(checksum);
            }
            version.merge();
            chunks.closeStream();
            
            // 3. Upload it
            if (FileUtil.checkIllegalName(item.getName())
                    || FileUtil.checkIllegalName(item.getPath().replace("/", ""))){
                logger.info("This filename contains illegal characters.");
                version.setSyncStatus(CloneItemVersion.SyncStatus.UNSYNC);
                version.merge();
            } else {
                logger.info("Indexer: Added to DB. Now Q file "+file+" at uploader ...");
                root.getProfile().getUploader().queue(version);
            }
            
        } catch (Exception ex) {
            logger.error("Could not index new file "+file+". IGNORING.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        } 
    }
    
    @Override
    public String toString() {
        return NewIndexSharedRequest.class.getSimpleName() + "[" + "file=" + file + "]";
    }    
    
}
