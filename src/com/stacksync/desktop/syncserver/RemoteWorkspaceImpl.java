package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.commons.models.CommitResult;
import com.stacksync.commons.omq.RemoteWorkspace;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.CommitInfo;
import java.util.ArrayList;
import java.util.List;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

public class RemoteWorkspaceImpl extends RemoteObject implements RemoteWorkspace {

    private final Logger logger = Logger.getLogger(RemoteWorkspaceImpl.class.getName());
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Config config = Config.getInstance();
    private CloneWorkspace workspace;
    private ChangeManager changeManager;

    public RemoteWorkspaceImpl(CloneWorkspace workspace, ChangeManager changeManager) {
        this.workspace = workspace;
        this.changeManager = changeManager;
    }

    @Override
    public void notifyCommit(CommitResult cr) {
        List<CommitInfo> listObjects = cr.getObjects();
        logger.info(" [x] Received in queue(" + workspace.getId() + ") '" + listObjects + "'");

        String fullReqId = cr.getRequestID();
        String deviceName = fullReqId.split("-")[0];
        List<Update> ul = new ArrayList<Update>();
        TempIdManager tempIdManager = new TempIdManager();

        for (CommitInfo obj : listObjects) {
            
            try {
                Update update = null;
                boolean committed = obj.isCommitSucceed();

                if (isMyCommit(deviceName) && committed) {
                    doActionCommitted(obj, tempIdManager);
                } else if (isMyCommit(deviceName) && !committed) {
                    update = doActionNotCommited(obj);
                } else {
                    update = Update.parse(obj.getMetadata(), workspace);
                }

                if (update != null) {
                    ul.add(update);
                }
            
            } catch (NullPointerException ex) {
                logger.info("Error parsing: " + obj, ex);
            }
        }

        if (!ul.isEmpty()) {
            logger.info("Queuing updates(" + ul.size() + ")");
            changeManager.queueUpdates(ul);
        }
    }
    
    private void doActionCommitted(CommitInfo commit, TempIdManager tempIdManager) {
        
        long version = commit.getCommittedVersion();
        long fileId = commit.getMetadata().getId();
        ItemMetadata itemMetadata = commit.getMetadata();
            
        Update update = Update.parse(itemMetadata, workspace);
            
        CloneFile existingVersion;
        Long tempId = itemMetadata.getTempId();
        if (tempId != null) {
            existingVersion = changeTempId(itemMetadata, tempIdManager);
        } else {
            existingVersion = db.getFileOrFolder(fileId, version);
        }

        if (existingVersion != null) {
            markAsUpdated(existingVersion, update);
        } else {
            logger.info("Exception: existing version is null");
        }
        
    }

    private Update doActionNotCommited(CommitInfo commit) {
       
        ItemMetadata itemMetadata = commit.getMetadata();
        
        Update update = Update.parse(itemMetadata, workspace);
        CloneFile existingVersion = db.getFileOrFolder(update.getFileId(), update.getVersion());
        if (existingVersion == null) {
            update.setConflicted(true);
            return update;
        } else {
            markAsUpdated(existingVersion, update);
        }
        return null;
    }
    
    private boolean isMyCommit(String deviceName) {
        return config.getDeviceName().compareTo(deviceName) == 0;
    }
    
    private CloneFile changeTempId(ItemMetadata itemMetadata, TempIdManager tempIdManager) {
        
        CloneFile localFile = db.getFileOrFolder(itemMetadata.getTempId(), itemMetadata.getVersion());
        
        return tempIdManager.changeTempId(localFile, itemMetadata.getId());
        
        /*CloneFile newFile = (CloneFile)localFile.clone();
        newFile.setServerUploadedAck(localFile.getServerUploadedAck());
        newFile.setServerUploadedTime(localFile.getServerUploadedTime());
        
        //newFile.setChunks(localFile.getChunks());
        newFile.setId(itemMetadata.getId());
        newFile.merge();
        //localFile.merge();
        //localFile.deleteFromDB();
        filesWithTempId.add(0, localFile);
        return newFile;*/
    }
    
    private void markAsUpdated(CloneFile cf, Update update) {
        cf.setServerUploadedAck(true);
        cf.setUpdated(update.getUpdated());
        cf.merge();
    }


}
