/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.workspace;

import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.sharing.WorkspaceController;
import com.stacksync.desktop.test.utils.DatabaseUtils;
import java.io.File;
import java.io.IOException;
import javax.persistence.EntityManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cotes
 */
public class WorkspaceControllerTest {
    
    private static WorkspaceController workspaceController;
    private static DatabaseHelper databaseHelper;
    private static EntityManager entityManager;
    
    public WorkspaceControllerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws ConfigException {
        workspaceController = WorkspaceController.getInstance();
        databaseHelper = DatabaseHelper.getInstance();
        
        File configFolder = new File("workspace_test");
        // Create and copy config file
        File configFile = DatabaseUtils.prepareConfigFile();
        databaseHelper.initializeDatabase(configFolder.getAbsolutePath(), DatabaseUtils.getDBConfigNode(configFile));
        entityManager = databaseHelper.getEntityManager();
    }
    
    @AfterClass
    public static void tearDownClass() {
        File configFolder = new File("database_test");
        try {
            FileUtils.deleteDirectory(configFolder);
        } catch (IOException ex) { }
    }
    
    @Before
    public void setUp() {
        CloneWorkspace defaultW = createWorkspace("wp1", "default", "/", true);
        CloneWorkspace sharedW = createWorkspace("shared", "shared_folder", "/shared_folder", false);
    }
    
    private CloneWorkspace createWorkspace(String id, String name, String path, boolean isDefault) {
        CloneWorkspace workspace = new CloneWorkspace();
        workspace.setDefaultWorkspace(isDefault);
        workspace.setId(id);
        workspace.setName(name);
        workspace.setLocalLastUpdate(1);
        workspace.setRemoteRevision(1);
        workspace.setOwner("me");
        workspace.setEncrypted(false);
        workspace.setPathWorkspace(path);
        persist(workspace);
        
        return workspace;
    }
    
    @After
    public void tearDown() {
        CloneWorkspace workspace = entityManager.find(CloneWorkspace.class, "wp1");
        CloneWorkspace workspace2 = entityManager.find(CloneWorkspace.class, "shared");
        
        entityManager.getTransaction().begin();
        entityManager.remove(workspace);
        entityManager.remove(workspace2);
        entityManager.getTransaction().commit();
    }

    @Test
    public void createNewWorkspaceTest() {
        
        CloneWorkspace sharedWorkspace = databaseHelper.getWorkspace("shared");
        
        Update update = new Update();
        update.setFileId(1L);
        update.setVersion(1);
        update.setParentFileId(null);
        update.setStatus(CloneItemVersion.Status.NEW);
        update.setChecksum(0);
        update.setFileSize(0);
        update.setFolder(true);
        update.setName("shared_folder");
        update.setWorkpace(sharedWorkspace);
        update.setServerUploaded(true);
        
        Folder root = new Folder();
        File rootFile = new File("workspace_test");
        root.setLocalFile(rootFile);
        
        workspaceController.createNewWorkspace(sharedWorkspace, update, root);
        
        CloneItem item = databaseHelper.getFileOrFolder(1L);
        assert item.getWorkspace().getId().equals("shared");
        assert item.getId().equals(1L);
        assert item.isWorkspaceRoot();
        assert item.isFolder();
        
        item.remove();
    }
    
    public void persist(Object o){
        entityManager.getTransaction().begin();
        entityManager.persist(o);
        entityManager.getTransaction().commit();
    }
}
