package com.stacksync.desktop.syncserver;

import com.ast.cloudABE.GUI.UIUtils;
import com.ast.cloudABE.cloudABEClient.CloudInvitedABEClientAdapter;
import com.ast.cloudABE.kpabe.SystemKey;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.models.abe.AccessTree;
import com.stacksync.commons.models.abe.KPABESecretKey;
import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.notifications.UpdateWorkspaceNotification;
import com.stacksync.commons.omq.RemoteClient;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Account;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.sharing.PasswordDialog;
import com.stacksync.desktop.sharing.WorkspaceController;
import com.stacksync.desktop.util.AccessTreeConverter;
import java.awt.Frame;
import java.io.File;
import java.util.logging.Level;
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
        newWorkspace.setEncrypted(spn.isEncrypted());
        newWorkspace.setAbeEncrypted(spn.isAbeEncrypted());
        newWorkspace.setShared(true);
        User owner = new User(spn.getOwnerId());
        owner.setName(spn.getOwnerName());
        newWorkspace.setOwner(owner);
        
         // Save new workspace in DB
        CloneWorkspace cloneWorkspace = new CloneWorkspace(newWorkspace);
        
        if (cloneWorkspace.isAbeEncrypted()){
            
            Account myAccount = config.getProfile().getAccount();
            KPABESecretKey secretKeyCommons = spn.getABEKeys().get(myAccount.getEmail());
            
            if(secretKeyCommons != null){
                SystemKey publicKey = null;
            
                AccessTree accessTree = secretKeyCommons.getAccess_tree();

                com.ast.cloudABE.accessTree.AccessTree adaptedTree = new com.ast.cloudABE.accessTree.AccessTree(AccessTreeConverter.transformTreeFromCommonsToABEClient(accessTree));
                com.ast.cloudABE.kpabe.KPABESecretKey secretKey = new com.ast.cloudABE.kpabe.KPABESecretKey(secretKeyCommons.getLeaf_keys(),adaptedTree);

                String RESOURCES_PATH = Environment.getInstance().getDefaultUserConfigDir().getAbsolutePath()+"/conf/abe/";
                
                try {
                    CloudInvitedABEClientAdapter abeClient = new CloudInvitedABEClientAdapter(RESOURCES_PATH);
                    abeClient.setupABESystem(0, secretKey.getAccess_tree().toString(), publicKey, secretKey);
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(RemoteClientImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                /* It is the data owner! Not considered case by the moment. We considered that data owner is exclusive for an specific device. 
                   Multidevice support should be considered */
            }

        }
                
        // If encrypted get the password
        /*String password = null;
        if (spn.isEncrypted()) {
            password = getPassword(spn.getFolderName());
            
            if (password == null) {
                // Do not do anything
                return;
            }
        }
        cloneWorkspace.setPassword(password);*/
        
        if (isMyWorkspace(cloneWorkspace))  {
            cloneWorkspace.merge();
        
            CloneFile sharedFolder = db.getFileOrFolder(spn.getItemId());
            WorkspaceController.getInstance().changeFolderWorkspace(cloneWorkspace, sharedFolder);
        }
        
        try {
            config.getProfile().addNewWorkspace(cloneWorkspace);
        } catch (Exception e) {
            logger.error("Error trying to listen new workspace: "+e);
        }
        
    }
    
    private boolean isMyWorkspace(CloneWorkspace workspace) {
        
        boolean myWorkspace = false;
        String me = db.getDefaultWorkspace().getOwner();
        if (workspace.getOwner().equals(me)) {
            myWorkspace = true;
        }
        
        return myWorkspace;
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
        if (remote.getParentId() != null) {
            CloneFile parentCF = db.getFileOrFolder(remote.getParentId());
            remote.setPathWorkspace(parentCF.getFile().getAbsolutePath()+ File.separator + uwn.getFolderName());
        } else {
            remote.setPathWorkspace("/"+uwn.getFolderName());
        }
        
        WorkspaceController.getInstance().applyChangesInWorkspace(local, remote, true);
        
    }
    
    private String getPassword(String folderName) {
        
        PasswordDialog dialog = new PasswordDialog(new Frame(), true, folderName);
        dialog.setVisible(true);
        return dialog.getPassword();
        
    }
    
    private boolean isMyCommit(String deviceName) {
        return config.getDeviceName().compareTo(deviceName) == 0;
    }
}
