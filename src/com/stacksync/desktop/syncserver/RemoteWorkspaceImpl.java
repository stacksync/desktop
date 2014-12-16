package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.commons.notifications.CommitNotification;
import com.stacksync.commons.omq.RemoteWorkspace;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.CommitInfo;
import com.stacksync.desktop.gui.server.Desktop;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

public class RemoteWorkspaceImpl extends RemoteObject implements RemoteWorkspace {

    private final Logger logger = Logger.getLogger(RemoteWorkspaceImpl.class.getName());
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Config config = Config.getInstance();
    private CloneWorkspace workspace;
    private ChangeManager changeManager;
    private Desktop desktop;

    public RemoteWorkspaceImpl(CloneWorkspace workspace, ChangeManager changeManager) {
        this.workspace = workspace;
        this.changeManager = changeManager;
        this.desktop = Desktop.getInstance();
    }

    @Override
    public void notifyCommit(CommitNotification cr) {
        List<CommitInfo> listObjects = cr.getObjects();
        logger.info(" [x] Received in queue(" + workspace.getId() + ") '" + listObjects + "'");

        Hashtable<Long, Long> temporalsId = new Hashtable<Long, Long>();
        
        String fullReqId = cr.getRequestId();
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
                } else if (committed){
                    update = Update.parse(obj.getMetadata(), workspace);
                }

                if (update != null) {
                    ul.add(update);
                }
                
                Long tempId = obj.getMetadata().getTempId();
                if (tempId != null) {
                    temporalsId.put(tempId, obj.getMetadata().getId());
                }
            
            } catch (NullPointerException ex) {
                logger.info("Error parsing: " + obj, ex);
            }
        }
        
        if (!temporalsId.isEmpty()) {
            changeTempIdFromUncommitedItems(temporalsId, tempIdManager);
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
            
        CloneFile existingVersion;
        Long tempId = itemMetadata.getTempId();
        if (tempId != null) {
            existingVersion = changeTempId(itemMetadata, tempIdManager);
        } else {
            existingVersion = db.getFileOrFolder(fileId, version);
        }

        if (existingVersion != null) {
            markAsUpdated(existingVersion);
        } else {
            logger.info("Exception: existing version is null");
        }
        
        this.desktop.touch(existingVersion.getAbsolutePath(), CloneFile.SyncStatus.UPTODATE);
    }

    private Update doActionNotCommited(CommitInfo commit) {
       
        ItemMetadata itemMetadata = commit.getMetadata();
        
        Update update = Update.parse(itemMetadata, workspace);
        CloneFile existingVersion = db.getFileOrFolder(update.getFileId(), update.getVersion());
        if (existingVersion == null) {
            update.setConflicted(true);
            this.desktop.touch(existingVersion.getAbsolutePath(), CloneFile.SyncStatus.UNSYNC);
            return update;
        } else {
            markAsUpdated(existingVersion);
        }
        this.desktop.touch(existingVersion.getAbsolutePath(), CloneFile.SyncStatus.UNSYNC);
        return null;
    }
    
    private boolean isMyCommit(String deviceName) {
        return config.getDeviceName().compareTo(deviceName) == 0;
    }
    
    private CloneFile changeTempId(ItemMetadata itemMetadata, TempIdManager tempIdManager) {
        
        CloneFile localFile = db.getFileOrFolder(itemMetadata.getTempId(), itemMetadata.getVersion());
        
        return tempIdManager.changeTempId(localFile, itemMetadata.getId());
        
    }
    
    private void markAsUpdated(CloneFile cf) {
        cf.setServerUploadedAck(true);
        cf.merge();
    }

    private void changeTempIdFromUncommitedItems(Hashtable<Long, Long> tempIds, TempIdManager tempIdManager) {
        
        Enumeration<Long> temps = tempIds.keys();
        while (temps.hasMoreElements()) {
            Long tempId = temps.nextElement();
            List<CloneFile> files = db.getFileVersions(tempId);
            tempIdManager.changeTempIdFromUncommitedItems(files, tempIds.get(tempId));
        }
    }

}
