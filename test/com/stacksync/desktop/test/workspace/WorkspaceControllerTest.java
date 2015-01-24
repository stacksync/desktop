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
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        CloneWorkspace defaultWorkspace = createWorkspace("wp1", "default", "/", true);
        CloneWorkspace sharedWworkspace = createWorkspace("shared", "shared_folder", "/shared_folder", false);
        
        createFolder1(defaultWorkspace);
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
    
    private void createFolder1(CloneWorkspace workspace) {
        CloneItem item = new CloneItem();
        item.setId(1L);
        item.setName("shared_folder");
        item.setFolder(true);
        item.setMimetype("folder");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.generatePath();
        ArrayList<CloneItemVersion> versions = new ArrayList<CloneItemVersion>();
        
        CloneItemVersion version1 = new CloneItemVersion();
        version1.setVersion(1);
        version1.setSize(4);
        version1.setChecksum(4);
        version1.setItem(item);
        version1.setStatus(CloneItemVersion.Status.NEW);
        version1.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version1.setServerUploadedAck(true);
        versions.add(version1);
        
        item.setLatestVersionNumber(1);
        item.setVersions(versions);
        persist(item);
        
        createChild1(workspace, item);
        createChildDeleted1(workspace, item);
    }
    
    private void createChild1(CloneWorkspace workspace, CloneItem folder) {
        CloneItem item = new CloneItem();
        item.setId(2L);
        item.setName("testfile");
        item.setFolder(false);
        item.setMimetype("file");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.setParent(folder);
        item.generatePath();
        
        CloneItemVersion version1 = new CloneItemVersion();
        version1.setVersion(1);
        version1.setSize(4);
        version1.setChecksum(4);
        version1.setItem(item);
        version1.setStatus(CloneItemVersion.Status.NEW);
        version1.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version1.setServerUploadedAck(true);
        item.addVersion(version1);
        
        persist(item);
        
    }
    
    private void createChildDeleted1(CloneWorkspace workspace, CloneItem folder) {
        CloneItem item = new CloneItem();
        item.setId(3L);
        item.setName("testfiledeleted");
        item.setFolder(false);
        item.setMimetype("file");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.setParent(folder);
        item.generatePath();
        
        CloneItemVersion version1 = new CloneItemVersion();
        version1.setVersion(1);
        version1.setSize(4);
        version1.setChecksum(4);
        version1.setItem(item);
        version1.setStatus(CloneItemVersion.Status.NEW);
        version1.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version1.setServerUploadedAck(true);
        item.addVersion(version1);
        
        CloneItemVersion version2 = new CloneItemVersion();
        version2.setVersion(2);
        version2.setSize(5);
        version2.setChecksum(5);
        version2.setItem(item);
        version2.setStatus(CloneItemVersion.Status.CHANGED);
        version2.setSyncStatus(CloneItemVersion.SyncStatus.LOCAL);
        version2.setServerUploadedAck(true);
        item.addVersion(version2);
        
        CloneItemVersion version3 = new CloneItemVersion();
        version3.setVersion(3);
        version3.setSize(5);
        version3.setChecksum(5);
        version3.setItem(item);
        version3.setStatus(CloneItemVersion.Status.DELETED);
        version3.setSyncStatus(CloneItemVersion.SyncStatus.LOCAL);
        version3.setServerUploadedAck(false);
        item.addVersion(version3);
        
        persist(item);
    }
    
    @After
    public void tearDown() {
        CloneWorkspace workspace = entityManager.find(CloneWorkspace.class, "wp1");
        CloneWorkspace workspace2 = entityManager.find(CloneWorkspace.class, "shared");
        CloneItem item1 = entityManager.find(CloneItem.class, 1L);
        CloneItem item2 = entityManager.find(CloneItem.class, 2L);
        CloneItem item3 = entityManager.find(CloneItem.class, 3L);
        
        entityManager.getTransaction().begin();
        entityManager.remove(item1);
        entityManager.remove(item2);
        entityManager.remove(item3);
        entityManager.remove(workspace);
        entityManager.remove(workspace2);
        entityManager.getTransaction().commit();
    }

    @Test
    public void createNewWorkspaceTest() {
        
        CloneWorkspace sharedWorkspace = databaseHelper.getWorkspace("shared");
        
        Update update = new Update();
        update.setFileId(10L);
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
        
        CloneItem item = databaseHelper.getFileOrFolder(10L);
        assert item.getWorkspace().getId().equals("shared");
        assert item.getId().equals(10L);
        assert item.isWorkspaceRoot();
        assert item.isFolder();
        
        item.remove();
    }
    
    @Test
    public void changeFolderWorkspaceTest() {
        CloneWorkspace sharedWorkspace = databaseHelper.getWorkspace("shared");
        CloneItem folder = databaseHelper.getFileOrFolder(1L);
        
        //Check that folder is inside default workspaces and is not root
        assert folder.getWorkspace().getId().equals("wp1");
        assert !folder.isWorkspaceRoot();
        
        workspaceController.changeFolderWorkspace(sharedWorkspace, folder);
        
        folder = databaseHelper.getFileOrFolder(1L);
        assert folder.isWorkspaceRoot();
        assert folder.getWorkspace().getId().equals(sharedWorkspace.getId());
        
        List<CloneItem> children = databaseHelper.getAllChildren(folder);
        assert children.size() == 2;
        assert children.get(0).getWorkspace().getId().equals(sharedWorkspace.getId());
    }
    
    public void persist(Object o){
        entityManager.getTransaction().begin();
        entityManager.persist(o);
        entityManager.getTransaction().commit();
    }
}
