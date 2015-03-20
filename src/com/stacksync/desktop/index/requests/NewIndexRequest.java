package com.stacksync.desktop.index.requests;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneChunk.CacheStatus;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion.Status;
import com.stacksync.desktop.db.models.CloneItemVersion.SyncStatus;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;

public class NewIndexRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(NewIndexRequest.class.getName());
    
    private File file;
    private CloneItem indexedItem;    
    private long checksum;

    public NewIndexRequest(Folder root, File file, CloneItem previousVersion, long checksum) {
        super(root);
  
        this.file = file;        
        this.indexedItem = previousVersion;
        this.checksum = checksum;
    }

    @Override
    public void process() {                
        logger.info("Indexer: Indexing new file "+file+" ...");
        
        // Check ignore and if file exists
        boolean allCorrect = doChecks();
        if (!allCorrect || alreadyProcessed()) {
            return;
        }
        
        // Check workspace
        CloneWorkspace defaultWorkspace = db.getDefaultWorkspace();
        File parentFile = FileUtil.getCanonicalFile(file.getParentFile());
        CloneItem parentCF = db.getFolder(root, parentFile);
        // If parent is null means the file is in the root folder (stacksync_folder).
        if (parentCF != null && !parentCF.getWorkspace().getId().equals(defaultWorkspace.getId())) {
            // File inside a shared workspace
            Indexer.getInstance().queueNewIndexShared(root, file, indexedItem, checksum);
            return;
        }
        
        // A shared folder will never arrive here!!
        // If arrives here means the file is in the default workspace! Apply normal process...
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        this.desktop.touch(file.getPath(), SyncStatus.SYNCING);
        
        // Create DB entry
        CloneItem newItem = (indexedItem == null) ? addNewVersion() : addChangedVersion();
        newItem.setParent(parentCF);
        newItem.setWorkspace(defaultWorkspace);
        
        // This will check if the file is inside a folder that isn't created.
        if (parentCF == null && !newItem.getPath().equals("/")) {
            Indexer.getInstance().queueNewIndex(root, file, indexedItem, checksum);
            return;
        }

        CloneItemVersion newVersion = newItem.getLatestVersion();
        newItem.setFolder(file.isDirectory());
        newVersion.setSize(file.length());
        
        newVersion.setLastModified(new Date(file.lastModified()));
        newVersion.setSyncStatus(SyncStatus.LOCAL);
        newItem.merge();
        
        // Process folder and files differently
        if (file.isDirectory()) {
            processFolder(newItem, newVersion);
        } else if (file.isFile()) {
            processFile(newItem, newVersion);
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
        
    private boolean doChecks() {
        // ignore file        
        if (FileUtil.checkIgnoreFile(root, file)) {
            logger.info("#ndexer: Ignore file "+file+" ...");
            return false;
        }
        
        // File vanished
        if (!file.exists()) {
            logger.warn("Indexer: Error indexing file "+file+": File does NOT exist. Ignoring.");            
            return false;
        }
        
        return true;
    }
    
    private boolean alreadyProcessed() {
        
        CloneItem dbFile = db.getFileOrFolder(root, file);
        if(dbFile == null){
            return false;
        }
        
        CloneItemVersion latestVersion = dbFile.getLatestVersion();
        if (latestVersion == null) {
            logger.error("Indexing file without latest version: "+dbFile.getId());
            return false;
        }
        
        if (indexedItem == null){
            logger.warn("Indexer: Error already NewIndexRequest processed. Ignoring.");
            return true;
        } else {
            if(latestVersion.getChecksum() == checksum){
                logger.warn("Indexer: Error already Indexed this version. Ignoring.");
                return true;
            }
        }

        if(dbFile.isFolder()){
            logger.warn("Indexer: Error already NewIndexRequest this folder. Ignoring.");
            return true;
        }
 
        return false;
    }
    

    private CloneItem addNewVersion() {
        CloneItem newItem = new CloneItem(root, file);  
        CloneItemVersion newVersion = new CloneItemVersion(file);
        newVersion.setStatus(Status.NEW);
        newItem.addVersion(newVersion);
        
        return newItem;
    }
    
    private CloneItem addChangedVersion() {        
        
        CloneItemVersion oldVersion = indexedItem.getLatestVersion();

        if (oldVersion.getSyncStatus() == SyncStatus.UNSYNC && oldVersion.getStatus() != Status.RENAMED) {
            if (oldVersion.getVersion() == 1) {
                oldVersion.setServerUploadedAck(false);
                oldVersion.setServerUploadedTime(null);
                oldVersion.setChecksum(0);
                oldVersion.setChunks(new ArrayList<CloneChunk>());
            } else {
                oldVersion.setStatus(Status.CHANGED);
                oldVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
                indexedItem.setMimetype(FileUtil.getMimeType(file));
            }
        } else {
            CloneItemVersion newVersion = (CloneItemVersion) oldVersion.clone();
            newVersion.setVersion(oldVersion.getVersion()+1);
            newVersion.setStatus(Status.CHANGED);
            newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
            indexedItem.setMimetype(FileUtil.getMimeType(indexedItem.getFile()));
            indexedItem.setLatestVersionNumber(newVersion.getVersion());
            indexedItem.addVersion(newVersion);
        }

        return indexedItem;
    }
    
    private void processFolder(CloneItem item, CloneItemVersion latestVersion) {        
        // Add rest of the DB stuff 
        latestVersion.setChecksum(0);
        
        if (FileUtil.checkIllegalName(item.getName())
                || FileUtil.checkIllegalName(item.getPath().replace("/", ""))){
            latestVersion.setSyncStatus(SyncStatus.UNSYNC);
        } else {
            latestVersion.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        }
        item.merge();
        
        // Analyze file tree (if directory) -- RECURSIVELY!!
        logger.info("Indexer: Indexing CHILDREN OF "+file+" ...");
        for (File child: file.listFiles()) {
            // Ignore .ignore files
            if (FileUtil.checkIgnoreFile(root, child)) {
                continue; 
            }
            
            // Do it!
            logger.info("Indexer: Parent: "+file+" / CHILD "+child+" ...");
            Indexer.getInstance().queueNewIndex(root, child, null, -1);
        }
    }

    private void processFile(CloneItem item, CloneItemVersion latestVersion) {
        try {
            // 1. Chunk it!
            FileChunk chunkInfo = null;

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);
            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();                

                // create chunk in DB (or retrieve it)
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), CacheStatus.CACHED);
                
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
                
                latestVersion.addChunk(chunk);
            }      
            
            logger.info("Indexer: saving chunks...");
            item.merge();
            logger.info("Indexer: chunks saved...");
            
            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) { // The last chunk holds the file checksum                
                latestVersion.setChecksum(chunkInfo.getFileChecksum()); 
            } else {
                latestVersion.setChecksum(checksum);
            }
            item.merge();
            chunks.closeStream();
            
            // 3. Upload it
            if (FileUtil.checkIllegalName(item.getName())
                    || FileUtil.checkIllegalName(item.getPath().replace("/", ""))){
                logger.info("This filename contains illegal characters.");
                latestVersion.setSyncStatus(SyncStatus.UNSYNC);
                item.merge();
            } else {
                logger.info("Indexer: Added to DB. Now Q file "+file+" at uploader ...");
                root.getProfile().getUploader().queue(latestVersion);
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
