package com.stacksync.desktop.sharing;

import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.repository.Update;
import java.io.File;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class WorkspaceController {
    
    private static WorkspaceController instance = null;
    private static DatabaseHelper db = DatabaseHelper.getInstance();
    private static final Logger logger = Logger.getLogger(WorkspaceController.class.getName());
    
    public static WorkspaceController getInstance() {
        if (instance ==  null) {
            instance = new WorkspaceController();
        }
        
        return instance;
    }
    
    public void createNewWorkspace(CloneWorkspace newWorkspace, Update update, Folder root) {
        
        if (newWorkspace.isDefaultWorkspace()) {
            // Don't create default wp
            return;
        }
        
        // Create workspace root folder
        File folder = new File(root.getLocalFile().getAbsolutePath()
                + newWorkspace.getPathWorkspace());
        
        // Create "dummy" clone file in DB
        saveWorkspaceRootFolder(newWorkspace, folder, update, root);
        
        // Finally create the FS folder
        folder.mkdir();
        
    }
    
    private void saveWorkspaceRootFolder(CloneWorkspace newWorkspace, File folder, Update update, Folder root) {
        
        CloneItem rootFolder = new CloneItem(root, folder);
        rootFolder.setId(update.getFileId());
        rootFolder.setUsingTempId(false);
        rootFolder.setWorkspaceRoot(true);
        rootFolder.setWorkspace(newWorkspace);
        rootFolder.setFolder(true);
        if (newWorkspace.getParentId() != null) {
            rootFolder.setParent(db.getFileOrFolder(newWorkspace.getParentId()));
        }
        
        CloneItemVersion rootVersion = new CloneItemVersion();
        rootVersion.setVersion(update.getVersion());
        rootVersion.setStatus(update.getStatus());
        rootVersion.setServerUploadedAck(true);   // Don't commit this!!
        rootVersion.setSize(0);
        rootVersion.setServerUploadedTime(update.getServerUploadedTime());
        rootVersion.setLastModified(new Date(folder.lastModified()));
        rootVersion.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        rootVersion.setChecksum(update.getChecksum());
        rootVersion.setItem(rootFolder);
       
        rootFolder.addVersion(rootVersion);
        rootFolder.merge();
    }
    
    public void changeFolderWorkspace(CloneWorkspace workspace, CloneItem folder) {
        
        folder.setWorkspace(workspace);
        folder.setWorkspaceRoot(true);
        
        List<CloneItem> children = db.getChildren(folder);
        this.changeWorkspaceRecursively(children, workspace);
        
        folder.merge();
    }
    
    private void changeWorkspaceRecursively(List<CloneItem> files, CloneWorkspace workspace) {
        for (CloneItem file : files) {
            file.setWorkspace(workspace);
            if (file.isFolder()) {
                this.changeWorkspaceRecursively(db.getChildren(file), workspace);
            }
            
            file.merge();
        }
    }

    public boolean applyChangesInWorkspace(CloneWorkspace local, CloneWorkspace remote, boolean uploaded) {
        
        boolean changed = false;
        
        if (!local.getRemoteRevision().equals(remote.getRemoteRevision())) {
            logger.info("New remote revision in workspace.");
            changed = true;
        }
        
        if (workspaceIsRenamed(local, remote)) {
            logger.info("New name in workspace. Renaming...");
            changeWorkspaceName(local, remote, uploaded);
            changed = true;
        }
        
        if (workspaceIsMoved(local,remote)) {
            logger.info("New parent in workspace. Moving...");
            changeWorkspaceParent(local, remote, uploaded);
            changed = true;
        }
        
        return changed;
    }
    
    private boolean workspaceIsRenamed(CloneWorkspace local, CloneWorkspace remote) {
        return !local.getName().equals(remote.getName());
    }

    private boolean workspaceIsMoved(CloneWorkspace local, CloneWorkspace remote) {
        
        Long currentParent = local.getParentId();
        Long newParent = remote.getParentId();
        
        if (currentParent == null && newParent == null) {
            return false;
        }
        
        if (currentParent != null && !currentParent.equals(newParent)) {
            return true;
        }
        
        return newParent != null && !newParent.equals(currentParent);
    }
    
    public void changeWorkspaceName(CloneWorkspace local, CloneWorkspace remote, boolean uploaded) {
        
        CloneItem workspaceRootFolder = db.getWorkspaceRoot(local.getId());
        
        String dir = workspaceRootFolder.getAbsolutePath();
        File folder = new File(dir);
        File newFolder = new File(folder.getParentFile()+File.separator+remote.getName());
        
        // Updtae workspace DB
        logger.info("Changing name and path in local CloneWorkspace.");
        local.setPathWorkspace(remote.getPathWorkspace());
        local.setName(remote.getName());
        local.merge();
        
        updateWorkspaceName(workspaceRootFolder, remote.getName(), uploaded);
        updateWorkspaceFiles(workspaceRootFolder);
        
        if (uploaded)
            folder.renameTo(newFolder);
    }

    private void updateWorkspaceName(CloneItem workspaceRootFolder, String newName, boolean uploaded) {
        
        CloneItemVersion latestVersion = workspaceRootFolder.getLatestVersion();
        CloneItemVersion newVersion = (CloneItemVersion)latestVersion.clone();
        
        newVersion.setServerUploadedAck(uploaded);
        newVersion.setServerUploadedTime(latestVersion.getServerUploadedTime());
        newVersion.setVersion(latestVersion.getVersion()+1);
        newVersion.setStatus(CloneItemVersion.Status.RENAMED);
        newVersion.setItem(workspaceRootFolder);
        workspaceRootFolder.setName(newName);
        
        workspaceRootFolder.getVersions().add(newVersion);
        workspaceRootFolder.setLatestVersionNumber(latestVersion.getVersion()+1);
        logger.info("Merging new dummy CloneFile with workspace root.");
        newVersion.merge();
    }

    private void updateWorkspaceFiles(CloneItem workspaceRootFolder) {
        
        logger.info("Updating files path but don't upload them!");
        List<CloneItem> files = db.getWorkspaceFiles(workspaceRootFolder.getWorkspace().getId());
        
        for (CloneItem file : files) {
            file.generatePath();
            file.merge();
        }
    }

    public void changeWorkspaceParent(CloneWorkspace local, CloneWorkspace remote, boolean uploaded) {
        
        CloneItem workspaceRootFolder = db.getWorkspaceRoot(local.getId());
        
        String dir = workspaceRootFolder.getAbsolutePath();
        File folder = new File(dir);
        File newFolder;
        CloneItem workspaceParentFolder;
        
        if (remote.getParentId() == null) {
            // Move shared folder to the root
            workspaceParentFolder = null;
            newFolder = new File(workspaceRootFolder.getRoot().getLocalFile().getAbsoluteFile()+File.separator+remote.getName());
        } else {
            // Move shared folder to another folder
            workspaceParentFolder = db.getFileOrFolder(remote.getParentId());
            newFolder = new File(workspaceParentFolder.getFile().getAbsolutePath()+File.separator + remote.getName());
        }
        
        // Updtae workspace DB
        logger.info("Changing name and path in local CloneWorkspace.");
        local.setPathWorkspace(remote.getPathWorkspace());
        local.setName(remote.getName());
        local.setParentId(remote.getParentId());
        local.merge();
        
        updateWorkspaceParent(workspaceRootFolder, workspaceParentFolder, uploaded);
        updateWorkspaceFiles(workspaceRootFolder);
        
        if (uploaded)
            folder.renameTo(newFolder);
    }
    
    private void updateWorkspaceParent(CloneItem workspaceRootFolder, CloneItem parent, boolean uploaded) {
        
        CloneItemVersion latestVersion = workspaceRootFolder.getLatestVersion();
        CloneItemVersion newVersion = (CloneItemVersion) latestVersion.clone();
        
        newVersion.setServerUploadedAck(uploaded);
        newVersion.setServerUploadedTime(latestVersion.getServerUploadedTime());
        newVersion.setVersion(latestVersion.getVersion()+1);
        newVersion.setStatus(CloneItemVersion.Status.RENAMED);
        workspaceRootFolder.addVersion(newVersion);
        workspaceRootFolder.setParent(parent);
        workspaceRootFolder.generatePath();
        
        logger.info("Merging new dummy CloneFile with workspace root.");
        workspaceRootFolder.merge();
    }
    
}