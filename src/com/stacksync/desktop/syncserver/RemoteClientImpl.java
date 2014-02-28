package com.stacksync.desktop.syncserver;

import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.sharing.SharingController;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

public class RemoteClientImpl extends RemoteObject implements RemoteClient {

    private final Logger logger = Logger.getLogger(RemoteClientImpl.class.getName());
    private final Config config = Config.getInstance();
    private final SharingController sharingController = SharingController.getInstance();
    
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
        
        try {
            config.getProfile().addNewWorkspace(cloneWorkspace);
            sharingController.createNewWorkspace(cloneWorkspace, spn.getFolderName());
        } catch (Exception e) {
            logger.error("Error trying to listen new workspace: "+e);
        }
        
        System.out.println(spn.toString());
    }

}
