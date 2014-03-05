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
                boolean changed = applyChangesInWorkspace(localWorkspaces.get(w.getId()), w);
                if (changed) {
                    localWorkspaces.put(w.getId(), w);
                }
            }else{
                // new workspace, let's create the workspace folder
                createNewWorkspace(w);
                // save it in DB
                w.merge();
                localWorkspaces.put(w.getId(), w);
            }

        }

        return localWorkspaces;
    }
    

    
    private boolean applyChangesInWorkspace(CloneWorkspace local, CloneWorkspace remote) {
        
        boolean changed = false;
        
        if (!local.getRemoteRevision().equals(remote.getRemoteRevision())) {
            logger.info("New remote revision in workspace.");
            changed = true;
        }
        
        if (!local.getName().equals(remote.getName())) {
            logger.info("New name in workspace. Renaming...");
            changeWorkspaceName(local.getId(), remote.getName());
            local.setPathWorkspace(remote.getPathWorkspace());
            local.setName(remote.getName());
            local.merge();
            changed = true;
        }
        
        /*
        if (local.getParentId().equals(remote.getParentId())) {
            logger.info("New parent in workspace. Moving...");
            // TODO
            * changed = true;
        }
        */
        
        return changed;
    }
    
    private void changeWorkspaceName(String id, String newName) {
        
        CloneFile workspaceRootFolder = db.getWorkspaceRoot(id);
        
        String dir = workspaceRootFolder.getAbsolutePath();
        File folder = new File(dir);
        File newFolder = new File(folder.getParentFile()+File.separator+newName);
        
        folder.renameTo(newFolder);
        
        updateWorkspaceName(workspaceRootFolder, newName);
        
    }
    
    public void createNewWorkspace(CloneWorkspace newWorkspace) {
        
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
        
        // Create "dummy" clone file in DB
        saveWorkspaceRootFolder(newWorkspace, folder);
        
        // Finally create the FS folder
        folder.mkdir();
        
    }

    private void saveWorkspaceRootFolder(CloneWorkspace newWorkspace, File folder) {
        saveWorkspaceRootFolder(newWorkspace, folder, 1, CloneFile.Status.NEW);
    }
    
    private void saveWorkspaceRootFolder(CloneWorkspace newWorkspace, File folder, int version, CloneFile.Status status) {
        
        Folder root = config.getProfile().getFolder();
        CloneFile rootFolder = new CloneFile(root, folder);
        rootFolder.setVersion(version);
        rootFolder.setStatus(status);
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

    private void updateWorkspaceName(CloneFile workspaceRootFolder, String newName) {
        CloneFile newVersion = (CloneFile)workspaceRootFolder.clone();
        
        newVersion.setServerUploadedAck(true);
        newVersion.setServerUploadedTime(workspaceRootFolder.getServerUploadedTime());
        newVersion.setVersion(workspaceRootFolder.getVersion()+1);
        newVersion.setStatus(CloneFile.Status.RENAMED);
        newVersion.setName(newName);
        
        newVersion.merge();
    }
    
}
