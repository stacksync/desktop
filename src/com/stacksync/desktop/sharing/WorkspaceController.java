package com.stacksync.desktop.sharing;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import java.io.File;
import java.util.Date;
import java.util.List;
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
    
    public void createNewWorkspace(CloneWorkspace newWorkspace) {
        
        if (newWorkspace.isDefaultWorkspace()) {
            // Don't create default wp
            return;
        }
        
        // Create workspace root folder
        File folder = new File(config.getProfile().getFolder().getLocalFile().getAbsolutePath()
                + newWorkspace.getPathWorkspace());
        
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
        
        if (newWorkspace.getParentId() != null) {
            rootFolder.setParent(db.getFileOrFolder(newWorkspace.getParentId()));
        }
        
        rootFolder.setWorkspace(newWorkspace);
        rootFolder.setFolder(true);
        rootFolder.setSize(0);
        rootFolder.setLastModified(new Date(folder.lastModified()));
        rootFolder.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        rootFolder.setChecksum(0);
        
        rootFolder.merge();
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
        
        if (newParent != null && !newParent.equals(currentParent)) {
            return true;
        }
        
        return false;
    }
    
    public void changeWorkspaceName(CloneWorkspace local, CloneWorkspace remote, boolean uploaded) {
        
        CloneFile workspaceRootFolder = db.getWorkspaceRoot(local.getId());
        
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

    private void updateWorkspaceName(CloneFile workspaceRootFolder, String newName, boolean uploaded) {
        
        CloneFile newVersion = (CloneFile)workspaceRootFolder.clone();
        
        newVersion.setServerUploadedAck(uploaded);
        newVersion.setServerUploadedTime(workspaceRootFolder.getServerUploadedTime());
        newVersion.setVersion(workspaceRootFolder.getVersion()+1);
        newVersion.setStatus(CloneFile.Status.RENAMED);
        newVersion.setName(newName);
        
        logger.info("Merging new dummy CloneFile with workspace root.");
        newVersion.merge();
    }

    private void updateWorkspaceFiles(CloneFile workspaceRootFolder) {
        
        logger.info("Updating files path but don't upload them!");
        List<CloneFile> files = db.getWorkspaceFiles(workspaceRootFolder.getWorkspace().getId());
        
        for (CloneFile file : files) {
            file.generatePath();
            file.merge();
        }
    }

    public void changeWorkspaceParent(CloneWorkspace local, CloneWorkspace remote, boolean uploaded) {
        
        CloneFile workspaceRootFolder = db.getWorkspaceRoot(local.getId());
        
        String dir = workspaceRootFolder.getAbsolutePath();
        File folder = new File(dir);
        File newFolder;
        CloneFile workspaceParentFolder;
        
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
    
    private void updateWorkspaceParent(CloneFile workspaceRootFolder, CloneFile parent, boolean uploaded) {
        
        CloneFile newVersion = (CloneFile)workspaceRootFolder.clone();
        
        newVersion.setServerUploadedAck(uploaded);
        newVersion.setServerUploadedTime(workspaceRootFolder.getServerUploadedTime());
        newVersion.setVersion(workspaceRootFolder.getVersion()+1);
        newVersion.setStatus(CloneFile.Status.RENAMED);
        newVersion.setParent(parent);
        newVersion.generatePath();
        
        logger.info("Merging new dummy CloneFile with workspace root.");
        newVersion.merge();
    }
    
}
