package com.stacksync.desktop.config.profile;

import com.ast.cloudABE.accessTree.AccessTree;
import com.ast.cloudABE.cloudABEClient.CABEConstants;
import com.ast.cloudABE.cloudABEClient.CloudInvitedABEClientAdapter;
import com.ast.cloudABE.kpabe.KPABE;
import com.ast.cloudABE.kpabe.KPABESecretKey;
import com.ast.cloudABE.kpabe.SystemKey;
import com.ast.cloudABE.util.AccessTreeIDsAdjuster;
import com.google.gson.Gson;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.encryption.BasicEncryption;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.Repository;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.encryption.AbeEncryption;
import com.stacksync.desktop.encryption.AbeInvitedEncryption;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.exceptions.NoPasswordException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.gui.sharing.PasswordDialog;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.repository.Uploader;
import com.stacksync.desktop.sharing.WorkspaceController;
import com.stacksync.desktop.syncserver.RemoteClientImpl;
import com.stacksync.desktop.syncserver.RemoteWorkspaceImpl;
import com.stacksync.desktop.syncserver.Server;
import com.stacksync.desktop.util.WinRegistry;
import com.stacksync.desktop.watch.local.LocalWatcher;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.desktop.watch.remote.RemoteWatcher;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import omq.common.broker.Broker;
import org.apache.log4j.Logger;

public class Profile implements Configurable {

    private static final Logger logger = Logger.getLogger(Profile.class.getName());
    private static final LocalWatcher localWatcher = LocalWatcher.getInstance();
    private Environment env = Environment.getInstance();
    private boolean active;
    private boolean enabled;
    private boolean initialized;
    private String name;
    private Repository repository;
    private Folder folder;
    private Uploader uploader;
    private RemoteWatcher remoteWatcher;
    private BrokerProperties brokerProps;
    private Broker broker;
    private Server server;
    private Account account;
    private HashMap<String, Encryption> workspaceEncryption;
    private String defaultWorkspacePassword;

    public Profile() {
        active = false;
        initialized = false;
        
        enabled = true;
        name = "(unknown)";
        account = new Account();

    }
    
    private void initialize() {
        
        if (initialized){
            return;
        }
        
        workspaceEncryption = new HashMap<String, Encryption>();
        uploader = new Uploader(this);
        remoteWatcher = new RemoteWatcher(this);
        initialized = true;
    }

    public boolean isActive() {
        return active;
    }

    @Deprecated
    public void setFactory() throws InitializationException {
        Config config = Config.getInstance();
        brokerProps = config.getBrokerProps();
        brokerProps.setRPCReply(config.getQueueName());
        
        try {
            if (broker == null) {
                broker = new Broker(brokerProps.getProperties());
            }
            if (server == null) {
                server = new Server(broker);
            }
        } catch (Exception ex) {
            throw new InitializationException(ex);
        }
    }

    public BrokerProperties getFactory() {
        return brokerProps;
    }

    public Server getServer() {
        return server;
    }

    public synchronized void setActive(boolean active) throws InitializationException, StorageConnectException {
        if (active == isActive()) {
            return;
        }
        
        initialize();

        // Activate
        if (active) {
            File localFolder = getFolder().getLocalFile();
            if (!localFolder.exists()) {
                localFolder.mkdirs();
            }

            setFactory();
            server.updateDevice(getAccountId());

            // Start threads 1/2
            uploader.start();
            ChangeManager changeManager = remoteWatcher.getChangeManager();
            changeManager.start();
            
            try {
                broker.bind(getAccountId(), new RemoteClientImpl());
            } catch (Exception ex) {
                logger.error("Error binding RemoteClient implementation: ", ex);
                throw new InitializationException(ex);
            }
            
            initializeWorkspaces(changeManager);

            // Start threads 2/2            
            localWatcher.watch(this);

            remoteWatcher.setServer(server);
            remoteWatcher.start();

            this.active = true;
        } else { // Deactivate
            localWatcher.unwatch(this);

            uploader.stop();
            remoteWatcher.stop();
            try {
                broker.stopBroker();
                broker = null;
                server = null;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            this.active = active;
        }
    }
    
    private void initializeWorkspaces(ChangeManager changeManager) throws InitializationException {
        
        // Get remote workspaces
        List<CloneWorkspace> remoteWorkspaces = new ArrayList<CloneWorkspace>();
        try {
            remoteWorkspaces = server.getWorkspaces(getAccountId());
        } catch (NoWorkspacesFoundException ex) {
            throw new InitializationException("Can't load the workspaces from syncserver: ", ex);
        }
        
        processDefaultWorkspace(changeManager, remoteWorkspaces);
        processSharedWorkspaces(changeManager, remoteWorkspaces);
    }
    
    private void processDefaultWorkspace(ChangeManager changeManager, List<CloneWorkspace> remoteWorkspaces)
            throws InitializationException {
        
        CloneWorkspace defaultWorkspace;
        try {
            defaultWorkspace = DatabaseHelper.getInstance().getDefaultWorkspace();
        } catch (NoResultException ex ){
            defaultWorkspace = null;
        }
        // Process default workspace
        boolean processedDefault = false;
        Iterator<CloneWorkspace> iterator = remoteWorkspaces.iterator();
        while (iterator.hasNext() && !processedDefault) {
            CloneWorkspace workspace = iterator.next();
            if (!workspace.isDefaultWorkspace()) {
                continue;
            }
            
            // TODO we don't know if default workspace is encrypted or not
            if (defaultWorkspace == null && defaultWorkspacePassword != null && !defaultWorkspacePassword.isEmpty()) {
                // Workspace encrypted
                workspace.setPassword(defaultWorkspacePassword);
                generateAndSaveEncryption(workspace.getId(), workspace.getPassword());
                workspace.merge();
            } else if (defaultWorkspace != null && defaultWorkspace.getPassword() != null) {
                // Workspace encrypted
                generateAndSaveEncryption(workspace.getId(), defaultWorkspace.getPassword());
            } else {
                workspace.merge();
            }
            
            bindWorkspace(workspace, changeManager);
            getAndQueueChanges(workspace, changeManager);
            iterator.remove();      // Remove it to avoid further process
            processedDefault = true;
        }
    }
    
    private void processSharedWorkspaces(ChangeManager changeManager, List<CloneWorkspace> remoteWorkspaces)
            throws InitializationException {
        
        // Get local workspaces from DB
        Map<String, CloneWorkspace> localWorkspaces = DatabaseHelper.getInstance().getWorkspaces();
        
        // Process workspaces individually
        WorkspaceController controller = WorkspaceController.getInstance();
        for(CloneWorkspace w: remoteWorkspaces){
            
            try {
                processWorkspace(w, controller, localWorkspaces);
            } catch (NoPasswordException ex) {
                logger.warn("No password for workspace "+w.toString());
                continue;
            }
            
            bindWorkspace(w, changeManager);
            getAndQueueSharedChanges(w, changeManager, controller);
            
            while (changeManager.queuesUpdatesIsWorking()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) { }
            }
            
        }
    }
    
    private void processWorkspace(CloneWorkspace workspace, WorkspaceController controller,
            Map<String, CloneWorkspace> localWorkspaces) throws InitializationException, NoPasswordException {
        
        // 1. Apply changes or create new workspaces
        if(localWorkspaces.containsKey(workspace.getId())){
            // search for changes in workspaces
            boolean changed = controller.applyChangesInWorkspace(localWorkspaces.get(workspace.getId()), workspace, true);
            if (changed) {
                localWorkspaces.put(workspace.getId(), workspace);
            }
            
            if (workspace.isEncrypted()) {
                generateAndSaveEncryption(workspace.getId(), workspace.getPassword());
            } else if (workspace.isAbeEncrypted()) {
                generateAndSaveAbeEncryption(localWorkspaces.get(workspace.getId()));
            }
        }else{
            
            if (workspace.isEncrypted()) {
                PasswordDialog dialog = new PasswordDialog(new java.awt.Frame(), true, workspace.getName());
                dialog.setVisible(true);
                String password = dialog.getPassword();
                if (password == null) {
                    throw new NoPasswordException();
                }
                workspace.setPassword(password);
                generateAndSaveEncryption(workspace.getId(), password);
            } else if (workspace.isAbeEncrypted()) {
                generateAndSaveAbeEncryption(workspace);
            }
            
            // new workspace, let's create the workspace folder
            //controller.createNewWorkspace(workspace);
            // save it in DB
            workspace.merge();
            localWorkspaces.put(workspace.getId(), workspace);
        }
        
    }
    
    private void generateAndSaveEncryption(String id, String password) throws InitializationException {
        try {
            // Create workspace encryption
            BasicEncryption encryption = new BasicEncryption(password);
            this.workspaceEncryption.put(id, encryption);
        } catch (ConfigException ex) {
            throw new InitializationException(ex);
        }
    }
    
    private void generateAndSaveAbeEncryption(CloneWorkspace workspace) throws InitializationException {
        try {
            
            Gson gson = new Gson(); 
            SystemKey publicKey = gson.fromJson(new String(workspace.getPublicKey()), SystemKey.class);
            
            if (isMyWorkspace(workspace)) {
               
                SystemKey masterKey = gson.fromJson(new String(workspace.getMasterKey()), SystemKey.class); 
                byte[] groupGenerator = workspace.getGroupGenerator();
                
                AbeEncryption encryption = new AbeEncryption(env.getAppConfDir().getPath() + "/abe/",
                publicKey, masterKey, groupGenerator);
                
                this.workspaceEncryption.put(workspace.getId(), encryption);
                
            } else {
                
                String RESOURCES_PATH = env.getAppConfDir().getPath() + "/abe/";
                        
                // 1. Load attributes
                ArrayList<String> attributeUniverse = CloudInvitedABEClientAdapter.getAttUniverseFromXML(RESOURCES_PATH + CABEConstants.XML_PATH);
                // 2. Create AccessTree class from the access structure string (i.e. Attr1 & Attr2)
                AccessTree accessTree = KPABE.setAccessTree(workspace.getAccessStructure());
                // 3. Adjust the IDs of the tree with the ones used in the attribute universe.
                accessTree = new AccessTree(AccessTreeIDsAdjuster.adjustAccessTreeIDs(accessTree, attributeUniverse));
                
                // First we create the secretKey from the workspace instance, but it lacks the access tree.
                KPABESecretKey secretKey = gson.fromJson(new String(workspace.getSecretKey()),  KPABESecretKey.class); 
                // Here we set the access tree for the secret key and we get the final SK necessary to the ABEEncryption.
                secretKey = new KPABESecretKey(secretKey.getLeaf_keys(),accessTree);
                
                AbeInvitedEncryption encryption = new AbeInvitedEncryption(env.getAppConfDir().getPath() + "/abe/",
                workspace.getAccessStructure(), publicKey, secretKey);
                
                this.workspaceEncryption.put(workspace.getId(), encryption);
            }   
        } catch (ConfigException ex) {
            throw new InitializationException(ex);
        }
    }
    
    private boolean isMyWorkspace(CloneWorkspace workspace) {
        
        boolean myWorkspace = false;
        String me = DatabaseHelper.getInstance().getDefaultWorkspace().getOwner();
        if (workspace.getOwner().equals(me)) {
            myWorkspace = true;
        }
        
        return myWorkspace;
    }
    
    private void bindWorkspace(CloneWorkspace workspace, ChangeManager changeManager) throws InitializationException{
        // 2. Listen to workspace queue
        try {
            // From now on, there will exist a new RemoteWorkspaceImpl which will be listen to the changes that are done in the SyncServer
            broker.bind(workspace.getId().toString(), new RemoteWorkspaceImpl(workspace, changeManager));
        } catch (Exception ex) {
            throw new InitializationException(ex);
        }
    }
    
    private void getAndQueueSharedChanges(CloneWorkspace workspace, ChangeManager changeManager, WorkspaceController controller) {
        // 3. Get changes and queue them
        List<Update> changes = server.getChanges(getAccountId(), workspace);
        
        // Get the root folder and create it
        Update rootFolder = changes.remove(0);
        controller.createNewWorkspace(workspace, rootFolder);
        
        changeManager.queueUpdates(changes);
    }
    
    private void getAndQueueChanges(CloneWorkspace workspace, ChangeManager changeManager) {
        // 3. Get changes and queue them
        List<Update> changes = server.getChanges(getAccountId(), workspace);
        changeManager.queueUpdates(changes);
    }
    
    public void addNewWorkspace(CloneWorkspace cloneWorkspace) throws Exception {
        
        ChangeManager changeManager = remoteWatcher.getChangeManager();
        changeManager.start();
        
        List<CloneWorkspace> workspace = new LinkedList<CloneWorkspace>();
        workspace.add(cloneWorkspace);
        
        this.processSharedWorkspaces(changeManager, workspace);
        
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getAccountId() {
        return this.account.getId().toString();
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public RemoteWatcher getRemoteWatcher() {
        return remoteWatcher;
    }

    public Uploader getUploader() {
        return uploader;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public Folder getFolder() {
        return this.folder;
    }
    
    public void setFolder(Folder folder) {
        this.folder = folder;
    }
    
    public Account getAccount(){
        return this.account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    public Encryption getEncryption(String workspaceId) {
        return this.workspaceEncryption.get(workspaceId);
    }

    public String getDefaultWorkspacePassword() {
        return defaultWorkspacePassword;
    }

    public void setDefaultWorkspacePassword(String defaultWorkspacePassword) {
        this.defaultWorkspacePassword = defaultWorkspacePassword;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        
        if (node == null) {
            return;
        }
        
        initialize();
        
        try {
            enabled = node.getBoolean("enabled");
            name = node.getProperty("name");

            // Repo
            repository = new Repository();
            repository.load(node.findChildByName("repository"));

            // Folder
            folder = new Folder(this);
            folder.load(node.findChildByXPath("folder"));
            
            account = new Account();
            account.load(node.findChildByName("account"));

        } catch (Exception e) {
            throw new ConfigException("Unable to load profile: " + e, e);
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("enabled", enabled);
        node.setProperty("name", name);
        
        repository.save(node.findOrCreateChildByXpath("repository", "repository"));
        folder.save(node.findOrCreateChildByXpath("folder", "folder"));
        account.save(node.findOrCreateChildByXpath("account", "account"));
    }

    public void stop() {
        
        if (!initialized){
            return;
        }

        uploader.stop();
        remoteWatcher.stop();
        try {
            broker.stopBroker();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public void savePathToRegistry(){
        
        if (env.getOperatingSystem() == Environment.OperatingSystem.Windows) {
            try {
                WinRegistry.writeWindowsRegistry(this.getFolder().getLocalFile().getPath());
            } catch (Exception ex) {
                logger.error("Could not write Windows registry", ex);
            }
        }
    }

    @Override
    public String toString() {
        return "Profile[active=" + active  + ", enabled= " + enabled + ", name=" + name + ", "
                + "repository=" + repository + ", folder:" + folder + "]";
    }
}