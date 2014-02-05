package com.stacksync.desktop.sharing;

import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.CloneWorkspace;
import java.io.File;

public class SharingController {
    
    private static SharingController instance;
    private Environment env = Environment.getInstance();
    private Config config = Config.getInstance();
    
    private SharingController() {
        
    }
    
    public static SharingController getInstance() {
        if (instance == null) {
            instance = new SharingController();
        }
        
        return instance;
    }
    
    /**
     * Creates a new folder named .nw_id_name in the root
     * folder. When the watcher watches this type of name
     * it will know that this is a new workspace and do
     * a different process.
     * @param workspace Workspace.
     * @param folderName  Folder name of the new workspace.
     */
    public void createNewWorkspace(CloneWorkspace workspace, String folderName) {
        
        String tempFolderName = ".nw_"+workspace.getId()+"_"+folderName;
        
        System.out.println(config.getProfile().getFolder().getLocalFile().getAbsolutePath());
        
        File tempFolder = new File(config.getProfile().getFolder().getLocalFile().getAbsolutePath()
                + "/" + tempFolderName);
        
        tempFolder.mkdir();
        
    }
    
}
