package com.stacksync.desktop.sharing;

import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.syncserver.Server;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class WorkspaceController {
    
    private static WorkspaceController instance = null;
    private static DatabaseHelper db = DatabaseHelper.getInstance();
    private Config config = Config.getInstance();
    private static final Logger logger = Logger.getLogger(WorkspaceController.class.getName());
    
    public static WorkspaceController getInstance() {
        if (instance ==  null) {
            instance = new WorkspaceController();
        }
        
        return instance;
    }
    
    public Map<String, CloneWorkspace> initializeWorkspaces(Profile profile)
            throws InitializationException{
                  
        List<CloneWorkspace> remoteWorkspaces = new ArrayList<CloneWorkspace>();
                                            
        try {
            Server server = profile.getServer();
            remoteWorkspaces = server.getWorkspaces(profile.getAccountId());
        } catch (NoWorkspacesFoundException ex) {
            throw new InitializationException("Can't load the workspaces from syncserver: ", ex);
        }

        Map<String, CloneWorkspace> localWorkspaces = db.getWorkspaces();
        
        
        for(CloneWorkspace w: remoteWorkspaces){                
            if(localWorkspaces.containsKey(w.getId())){
                // search for changes in workspaces
                applyChangesInWorkspace(localWorkspaces.get(w.getId()), w);
                localWorkspaces.put(w.getId(), w);
            }else{
                // new workspace, let's create the workspace folder
                createNewWorkspace(w);
                // save it in DB
                w.merge();
                localWorkspaces.put(w.getId(), w);
            }

            //save changes
            localWorkspaces.get(w.getId()).merge();
        }

        return localWorkspaces;
    }
    

    
    private void applyChangesInWorkspace(CloneWorkspace local, CloneWorkspace remote) {
        
        if (!local.getRemoteRevision().equals(remote.getRemoteRevision())) {
            logger.info("New remote revision in workspace.");
        }
        
        if (!local.getName().equals(remote.getName())) {
            logger.info("New name in workspace. Renaming...");
            changeWorkspaceName(local.getId(), local.getName(), remote.getName());
        }
        
        /*
        if (local.getParentId().equals(remote.getParentId())) {
            logger.info("New parent in workspace. Moving...");
            // TODO
        }
        */
    }
    
    private void changeWorkspaceName(String id, String from, String to) {
        
        CloneFile workspaceRootFolder = db.getWorkspaceRoot(id);
        
        String parentDir = workspaceRootFolder.getAbsoluteParentDirectory();
        parentDir = workspaceRootFolder.getAbsolutePath();
        File folder = new File(parentDir);
        File newFolder = new File(folder.getParentFile()+File.separator+to);
        
        folder.renameTo(newFolder);
    }
    
    private void createNewWorkspace(CloneWorkspace newWorkspace) {
        
        if (newWorkspace.getName().equals("default")) {
            // Don't create default wp
            return;
        }
        
        // This is not usefull since watcher is not activated
        /*// craete new forlder (.nw_id_name)
        SharingController controller = SharingController.getInstance();
        controller.createNewWorkspace(newWorkspace, newWorkspace.getName());*/
        
        // Create workspace root folder
        String folderName = newWorkspace.getName();
        
        File folder = new File(config.getProfile().getFolder().getLocalFile().getAbsolutePath()
                + "/" + folderName);
        
        folder.mkdir();
        
        saveWorkspaceRootFolder(newWorkspace, folder);
    }

    private void saveWorkspaceRootFolder(CloneWorkspace newWorkspace, File folder) {
        
        Folder root = config.getProfile().getFolder();
        CloneFile rootFolder = new CloneFile(root, folder);
        rootFolder.setVersion(1);
        rootFolder.setStatus(CloneFile.Status.NEW);
        rootFolder.setWorkspaceRoot(true);
        rootFolder.setServerUploadedAck(true);   // Don't commit this!!
        
        // TODO This is not necessary because shared folders will be on the root
        /*File parentFile = FileUtil.getCanonicalFile(folder.getParentFile());
        rootFolder.setParent(db.getFolder(root, parentFile));*/
        
        rootFolder.setWorkspace(newWorkspace);
        rootFolder.setFolder(true);
        rootFolder.setSize(0);
        rootFolder.setLastModified(new Date(folder.lastModified()));
        rootFolder.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        rootFolder.setChecksum(0);
        
        rootFolder.merge();
    }
    
}
