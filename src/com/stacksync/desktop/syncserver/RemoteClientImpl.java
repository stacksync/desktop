package com.stacksync.desktop.syncserver;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.notifications.UpdateWorkspaceNotification;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.sharing.WorkspaceController;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

public class RemoteClientImpl extends RemoteObject implements RemoteClient {

    private final Logger logger = Logger.getLogger(RemoteClientImpl.class.getName());
    private final Config config = Config.getInstance();
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    
    @Override
    public void notifyShareProposal(ShareProposalNotification spn) {
        
        Workspace newWorkspace = new Workspace(spn.getWorkspaceId());
        newWorkspace.setSwiftContainer(spn.getSwiftContainer());
        newWorkspace.setSwiftUrl(spn.getSwiftURL());
        newWorkspace.setName(spn.getFolderName());
        newWorkspace.setLatestRevision(0);
        User owner = new User(spn.getOwnerId());
        owner.setName(spn.getOwnerName());
        newWorkspace.setOwner(owner);
        
         // Save new workspace in DB
        CloneWorkspace cloneWorkspace = new CloneWorkspace(newWorkspace);
        cloneWorkspace.merge();
        
        WorkspaceController.getInstance().createNewWorkspace(cloneWorkspace);
        
        try {
            config.getProfile().addNewWorkspace(cloneWorkspace);
        } catch (Exception e) {
            logger.error("Error trying to listen new workspace: "+e);
        }
        
    }

    @Override
    public void notifyUpdateWorkspace(UpdateWorkspaceNotification uwn) {
        
        String fullReqId = uwn.getRequestId();
        String deviceName = fullReqId.split("-")[0];
        
        if (isMyCommit(deviceName)) {
            CloneFile rootCF = db.getWorkspaceRoot(uwn.getWorkspaceId().toString());
            rootCF.setServerUploadedAck(true);
            rootCF.merge();
            return;
        }
        
        CloneWorkspace local = db.getWorkspace(uwn.getWorkspaceId().toString());
        
        CloneWorkspace remote = local.clone();
        remote.setName(uwn.getFolderName());
        remote.setParentId(uwn.getParentItemId());
        remote.setPathWorkspace("/"+uwn.getFolderName());
        
        WorkspaceController.getInstance().applyChangesInWorkspace(local, remote, true);
        
    }
    
    private boolean isMyCommit(String deviceName) {
        return config.getDeviceName().compareTo(deviceName) == 0;
    }
}