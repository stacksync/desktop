package com.stacksync.desktop.test.db;

import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 *
 * @author cotes
 */
public class TestItem {
    
    private static DatabaseHelper databaseHelper;
    private static EntityManager entityManager;
    
    @BeforeClass
    public static void setUpClass() throws ConfigException {
        
        databaseHelper = DatabaseHelper.getInstance();
        
        File configFolder = new File("database_test");
        // Create and copy config file
        File configFile = prepareConfigFile();
        databaseHelper.initializeDatabase(configFolder.getAbsolutePath(), getDBConfigNode(configFile));
        entityManager = databaseHelper.getEntityManager();
    }
    
    public static File prepareConfigFile() {
        File configFolder = new File("database_test");
        configFolder.mkdir();
        
        File configFile = new File(configFolder.getAbsoluteFile() + File.separator + Constants.CONFIG_FILENAME);
        
        InputStream is = null;
        try {
            is = TestItem.class.getResourceAsStream(Constants.CONFIG_DEFAULT_FILENAME);

            FileUtil.writeFile(is, configFile);
        } catch (IOException e) {
            assert false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                assert false;
            }
        }
        
        return configFile;
    }
    
    public static ConfigNode getDBConfigNode(File configFile){
        
        ConfigNode node = null;
        InputStream configStream = null;
        try {
            configStream = new FileInputStream(configFile);
            node = parseConfigFile(configStream);
        } catch (FileNotFoundException ex) {
            assert false;
        } finally {
            try {
                if (configStream != null) {
                    configStream.close();
                }
            } catch (IOException ex) { }
        }
        
        return node;
    }
    
    public static ConfigNode parseConfigFile(InputStream configStream) {
        
        ConfigNode node = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(configStream);
            node = new ConfigNode(doc.getDocumentElement());
            node = node.findChildByName("database");
        } catch (Exception e) {
            assert false;
        }
        
        return node;
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
        CloneWorkspace workspace = new CloneWorkspace();
        workspace.setDefaultWorkspace(true);
        workspace.setId("wp1");
        workspace.setName("default");
        workspace.setLocalLastUpdate(1);
        workspace.setRemoteRevision(1);
        workspace.setOwner("me");
        workspace.setEncrypted(false);
        workspace.setPathWorkspace("/");
        workspace.setDefaultWorkspace(true);
        persist(workspace);
        
        createItem1(workspace);
        createItem2(workspace);
        createItem3(workspace);
        createFolder1(workspace);
        createSharedWorkspace();
    }
    
    private void createItem1(CloneWorkspace workspace) {
        CloneItem item = new CloneItem();
        item.setId(1L);
        item.setName("testfolder");
        item.setFolder(true);
        item.setMimetype("folder");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.generatePath();
        ArrayList<CloneItemVersion> versions = new ArrayList<CloneItemVersion>();
        
        CloneItemVersion version1 = new CloneItemVersion();
        version1.setVersion(1);
        version1.setSize(1);
        version1.setChecksum(1);
        version1.setItem(item);
        version1.setStatus(CloneItemVersion.Status.NEW);
        version1.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version1.setServerUploadedAck(true);
        versions.add(version1);
        
        CloneItemVersion version2 = new CloneItemVersion();
        version2.setVersion(2);
        version2.setSize(2);
        version2.setChecksum(2);
        version2.setItem(item);
        version2.setStatus(CloneItemVersion.Status.CHANGED);
        version2.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version2.setServerUploadedAck(true);
        versions.add(version2);
        
        CloneItemVersion version3 = new CloneItemVersion();
        version3.setVersion(3);
        version3.setSize(3);
        version3.setChecksum(3);
        version3.setItem(item);
        version3.setStatus(CloneItemVersion.Status.DELETED);
        version3.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version3.setServerUploadedAck(false);
        versions.add(version3);
        
        item.setLatestVersionNumber(3);
        item.setVersions(versions);
        persist(item);
    }
    
    private void createItem2(CloneWorkspace workspace) {
        CloneItem item = new CloneItem();
        item.setId(2L);
        item.setName("testfile");
        item.setFolder(false);
        item.setMimetype("file");
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
        
        CloneItemVersion version2 = new CloneItemVersion();
        version2.setVersion(2);
        version2.setSize(5);
        version2.setChecksum(5);
        version2.setItem(item);
        version2.setStatus(CloneItemVersion.Status.CHANGED);
        version2.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version2.setServerUploadedAck(true);
        versions.add(version2);
        
        item.setLatestVersionNumber(2);
        item.setVersions(versions);
        persist(item);
        
        CloneChunk chunk = new CloneChunk("checksum1", CloneChunk.CacheStatus.REMOTE);
        persist(chunk);
        version2.addChunk(chunk);
        persist(version2);
    }
    
    private void createItem3(CloneWorkspace workspace) {
        CloneItem item = new CloneItem();
        item.setId(3L);
        item.setName("testfile");
        item.setFolder(false);
        item.setMimetype("file");
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
        
        CloneItemVersion version2 = new CloneItemVersion();
        version2.setVersion(2);
        version2.setSize(5);
        version2.setChecksum(5);
        version2.setItem(item);
        version2.setStatus(CloneItemVersion.Status.DELETED);
        version2.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version2.setServerUploadedAck(false);
        versions.add(version2);
        
        item.setLatestVersionNumber(2);
        item.setVersions(versions);
        persist(item);
    }
    
    private void createFolder1(CloneWorkspace workspace) {
        CloneItem item = new CloneItem();
        item.setId(4L);
        item.setName("testfolder");
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
        item.setId(5L);
        item.setName("testfile");
        item.setFolder(false);
        item.setMimetype("file");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.setParent(folder);
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
        
        CloneChunk chunk = new CloneChunk("checksum1", CloneChunk.CacheStatus.REMOTE);
        version1.addChunk(chunk);
        persist(version1);
    }
    
    private void createChildDeleted1(CloneWorkspace workspace, CloneItem folder) {
        CloneItem item = new CloneItem();
        item.setId(6L);
        item.setName("testfiledeleted");
        item.setFolder(false);
        item.setMimetype("file");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.setParent(folder);
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
        
        CloneItemVersion version2 = new CloneItemVersion();
        version2.setVersion(2);
        version2.setSize(5);
        version2.setChecksum(5);
        version2.setItem(item);
        version2.setStatus(CloneItemVersion.Status.CHANGED);
        version2.setSyncStatus(CloneItemVersion.SyncStatus.LOCAL);
        version2.setServerUploadedAck(true);
        versions.add(version2);
        
        CloneItemVersion version3 = new CloneItemVersion();
        version3.setVersion(3);
        version3.setSize(5);
        version3.setChecksum(5);
        version3.setItem(item);
        version3.setStatus(CloneItemVersion.Status.DELETED);
        version3.setSyncStatus(CloneItemVersion.SyncStatus.LOCAL);
        version3.setServerUploadedAck(false);
        versions.add(version3);
        
        item.setLatestVersionNumber(3);
        item.setVersions(versions);
        persist(item);
    }
    
    private void createSharedWorkspace() {
        CloneWorkspace workspace = new CloneWorkspace();
        workspace.setDefaultWorkspace(false);
        workspace.setId("shared");
        workspace.setName("shared_folder");
        workspace.setLocalLastUpdate(1);
        workspace.setRemoteRevision(1);
        workspace.setOwner("me");
        workspace.setEncrypted(false);
        workspace.setPathWorkspace("/shared_folder");
        workspace.setDefaultWorkspace(false);
        persist(workspace);
        
        CloneItem item = new CloneItem();
        item.setId(10L);
        item.setName("shared_folder");
        item.setFolder(true);
        item.setMimetype("folder");
        item.setUsingTempId(false);
        item.setWorkspace(workspace);
        item.setWorkspaceRoot(true);
        item.setLatestVersionNumber(1);
        item.generatePath();
        ArrayList<CloneItemVersion> versions = new ArrayList<CloneItemVersion>();
        
        CloneItemVersion version1 = new CloneItemVersion();
        version1.setVersion(1);
        version1.setSize(1);
        version1.setChecksum(1);
        version1.setItem(item);
        version1.setStatus(CloneItemVersion.Status.NEW);
        version1.setSyncStatus(CloneItemVersion.SyncStatus.UPTODATE);
        version1.setServerUploadedAck(false);
        versions.add(version1);
        
        item.setVersions(versions);
        persist(item);
    }
    
    @After
    public void tearDown() {
        CloneWorkspace workspace = entityManager.find(CloneWorkspace.class, "wp1");
        CloneWorkspace workspace2 = entityManager.find(CloneWorkspace.class, "shared");
        CloneItem item = entityManager.find(CloneItem.class, 1L);
        CloneItem item2 = entityManager.find(CloneItem.class, 2L);
        CloneItem item3 = entityManager.find(CloneItem.class, 3L);
        CloneItem item4 = entityManager.find(CloneItem.class, 4L);
        CloneItem item5 = entityManager.find(CloneItem.class, 5L);
        CloneItem item6 = entityManager.find(CloneItem.class, 6L);
        CloneItem item7 = entityManager.find(CloneItem.class, 10L);
        CloneChunk chunk1 = entityManager.find(CloneChunk.class, "checksum1");
        
        entityManager.getTransaction().begin();
        entityManager.remove(item);
        entityManager.remove(item2);
        entityManager.remove(item3);
        entityManager.remove(item4);
        entityManager.remove(item5);
        entityManager.remove(item6);
        entityManager.remove(item7);
        entityManager.remove(chunk1);
        entityManager.remove(workspace);
        entityManager.remove(workspace2);
        entityManager.getTransaction().commit();
    }
    
    /*@Test
    public void createItemWithVersions() {
        System.out.println("Hola");
    }*/
    
    @Test
    public void getNoDeletedFiles() {
        List<CloneItem> items = databaseHelper.getFiles();
        assert items.size() == 4;
    }
    
    @Test
    public void getItemById() {
        CloneItem item = databaseHelper.getFileOrFolder(1L);
        assert item.getId().equals(1L);
    }
    
    @Test
    public void getFile(){
        Folder root = new Folder();
        File rootFile = new File("./database_test");
        root.setLocalFile(rootFile);
        
        File testFile = new File("./database_test/testfile");
        CloneItem item = databaseHelper.getFile(root, testFile);
        assert item.getName().equals("testfile");
        assert item.getId().equals(2L);
    }
    
    @Test
    public void getFolder(){
        Folder root = new Folder();
        File rootFile = new File("./database_test");
        root.setLocalFile(rootFile);
        
        File testFolder = new File("./database_test/testfolder");
        CloneItem folder = databaseHelper.getFolder(root, testFolder);
        assert folder.getId().equals(4L);
        
    }
    
    @Test
    public void getFileOrFolderWithRoot() {
        Folder root = new Folder();
        File rootFile = new File("./database_test");
        root.setLocalFile(rootFile);
        
        File testFolder = new File("./database_test/testfolder");
        CloneItem folder = databaseHelper.getFileOrFolder(root, testFolder);
        assert folder.getId().equals(4L);
        
        File testFile = new File("./database_test/testfile");
        CloneItem item = databaseHelper.getFileOrFolder(root, testFile);
        assert item.getName().equals("testfile");
        assert item.getId().equals(2L); 
    }
    
    @Test
    public void getFileOrFolderFromId() {
        CloneItem item = databaseHelper.getFileOrFolder(5L);
        assert item.getId().equals(5L);
    }
    
    @Test
    public void getChildrenTest() {
        Folder root = new Folder();
        File rootFile = new File("./database_test");
        root.setLocalFile(rootFile);
        
        File testFolder = new File("./database_test/testfolder");
        CloneItem folder = databaseHelper.getFolder(root, testFolder);
        
        List<CloneItem> children = databaseHelper.getChildren(folder);
        assert children.size() == 1;
        assert children.get(0).getId().equals(5L);
    }
    
    @Test
    public void getFilesFromStatus() {
        List<CloneItemVersion> localItemsVersions = databaseHelper.getFiles(CloneItemVersion.SyncStatus.LOCAL);
        assert localItemsVersions.size() == 2;
        
        CloneItemVersion version1 = localItemsVersions.remove(0);
        assert version1.getItem().getId().equals(6L) && version1.getVersion() == 2;
        
        CloneItemVersion version2 = localItemsVersions.remove(0);
        assert version2.getItem().getId().equals(6L) && version2.getVersion() == 3;
    }
    
    @Test
    public void getFileVersionCountTest() {
        Long value = databaseHelper.getFileVersionCount();
        if (value != null) {
            assert value.equals(3L);
        } else {
            assert false;
        }
    }
    
    @Test
    public void getWorkspaceFilesTest() {
        List<CloneItem> items = databaseHelper.getWorkspaceFiles("wp1");
        assert items.size() == 3;
    }
    
    @Test
    public void getWorkspaceTest() {
        CloneWorkspace w = databaseHelper.getWorkspace("wp1");
        assert w.getId().equals("wp1");
        assert w.getOwner().equals("me");
        assert w.getName().equals("default");
        
        CloneWorkspace w2 = databaseHelper.getWorkspace("shared");
        assert w2.getId().equals("shared");
        assert w2.getOwner().equals("me");
        assert w2.getName().equals("shared_folder");
    }
    
    @Test
    public void getWorkspacesUpdatesTest() {
        List<CloneItem> roots = databaseHelper.getWorkspacesUpdates();
        assert roots.size() == 1;
        assert roots.get(0).getId().equals(10L);
    }
    
    @Test
    public void getWorkspaceRootTest() {
        CloneItem root = databaseHelper.getWorkspaceRoot("shared");
        assert root.getId() == 10L;
        assert root.getName().equals("shared_folder");
    }
    
    @Test
    public void getDefaultWorkspaceTest() {
        CloneWorkspace defaultWorkspace = databaseHelper.getDefaultWorkspace();
        assert defaultWorkspace.isDefaultWorkspace();
        assert defaultWorkspace.getPathWorkspace().equals("/");
    }
    
    @Test
    public void getCloneItemsFromChunk() {
        CloneChunk chunk = new CloneChunk("checksum1", CloneChunk.CacheStatus.REMOTE);
        List<CloneItem> items = databaseHelper.getCloneFiles(chunk);
        assert items.size() == 2;
    }
    
    public void persist(Object o){
        entityManager.getTransaction().begin();
        entityManager.persist(o);
        entityManager.getTransaction().commit();
    }

}
