/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.omq.RemoteWorkspace;
import com.stacksync.syncservice.models.ItemMetadata;
import com.stacksync.syncservice.models.CommitInfo;
import java.util.ArrayList;
import java.util.List;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

public class RemoteWorkspaceImpl extends RemoteObject implements RemoteWorkspace {

    private final Logger logger = Logger.getLogger(RemoteWorkspaceImpl.class.getName());
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Config config = Config.getInstance();
    private Workspace workspace;
    private ChangeManager changeManager;

    public RemoteWorkspaceImpl(Workspace workspace, ChangeManager changeManager) {
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

        for (CommitInfo obj : listObjects) {
            boolean committed = obj.isCommitSucceed();
            Update update;
            try {
                
                if (committed) {
                    update = doActionCommitted(deviceName, obj);
                } else {
                    update = doActionNotCommited(deviceName, obj);
                }
                
                if (update != null) {
                    ul.add(update);
                }

            } catch (NullPointerException ex) {
                logger.info("Error parsing: " + obj, ex);
            }
        }

        if (ul != null && ul.size() > 0) {
            logger.info("Queuing updates(" + ul.size() + ")");
            changeManager.queueUpdates(ul);
        }
    }

    /*private Update doActionCommitted(String deviceName, CommitInfo commit) {
        
        long version = commit.getCommittedVersion();
        long fileId = commit.getMetadata().getId();
        ItemMetadata itemMetadata = commit.getMetadata();
            
        Update update = Update.parse(itemMetadata, workspace);
        
        if (isMyCommit(deviceName)) {
            
            CloneFile existingVersion;
            // If first version, set unique ID
            if (version == 1) {
                Long tempId = itemMetadata.getTempId();
                existingVersion = changeTempId(itemMetadata);
            } else {
                existingVersion = db.getFileOrFolder(fileId, version);
            }
            
            if (existingVersion != null) {
                markAsUpdated(existingVersion, update);
            } else {
                logger.info("Exception: existing version is null");
            }
        } else {
            // It is not my commit.
            return update;
        }
        
        return null;
    }*/
    
    private Update doActionCommitted(String deviceName, CommitInfo commit) {
        
        long version = commit.getCommittedVersion();
        long fileId = commit.getMetadata().getId();
        ItemMetadata itemMetadata = commit.getMetadata();
            
        Update update = Update.parse(itemMetadata, workspace);
        CloneFile existingVersion = db.getFileOrFolder(fileId, version);
        if (isMyCommit(deviceName)) {
            if (existingVersion != null) {
                markAsUpdated(existingVersion, update);
            } else {
                logger.info("Exception: existing version is null");
            }
        } else {
            // It is not my commit.
            return update;
        }
        
        return null;
    }

    private Update doActionNotCommited(String deviceName, CommitInfo commit) {
       
        ItemMetadata itemMetadata = commit.getMetadata();
        
        if (isMyCommit(deviceName)) {
            Update update = Update.parse(itemMetadata, workspace);
            CloneFile existingVersion = db.getFileOrFolder(update.getFileId(), update.getVersion());
            if (existingVersion == null) {
                update.setConflicted(true);
                return update;
            } else {
                markAsUpdated(existingVersion, update);
            }
        }
        return null;
    }
    
    private boolean isMyCommit(String deviceName) {
        return config.getDeviceName().compareTo(deviceName) == 0;
    }
    
    private CloneFile changeTempId(ItemMetadata itemMetadata) {
        return null;
    }
    
    private void markAsUpdated(CloneFile cf, Update update) {
        cf.setServerUploadedAck(true);
        cf.setUpdated(update.getUpdated());
        cf.merge();
    }
}
