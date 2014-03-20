package com.stacksync.desktop.config.profile;

import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.config.Encryption;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.Repository;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
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
            if (defaultWorkspace == null && !defaultWorkspacePassword.isEmpty()) {
                // Workspace encrypted
                workspace.setPassword(defaultWorkspacePassword);
                generateAndSaveEncryption(workspace.getId(), workspace.getPassword());
                workspace.merge();
            } else if (defaultWorkspace != null && defaultWorkspace.getPassword() != null) {
                // Workspace encrypted
                generateAndSaveEncryption(workspace.getId(), defaultWorkspace.getPassword());
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
            processWorkspace(w, controller, localWorkspaces);
            bindWorkspace(w, changeManager);
            getAndQueueChanges(w, changeManager);
            
            while (changeManager.queuesUpdatesIsWorking()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) { }
            }
            
        }
    }
    
    private void processWorkspace(CloneWorkspace workspace, WorkspaceController controller,
            Map<String, CloneWorkspace> localWorkspaces) throws InitializationException {
        
        // 1. Apply changes or create new workspaces
        if(localWorkspaces.containsKey(workspace.getId())){
            // search for changes in workspaces
            boolean changed = controller.applyChangesInWorkspace(localWorkspaces.get(workspace.getId()), workspace, true);
            if (changed) {
                localWorkspaces.put(workspace.getId(), workspace);
            }
            
            if (workspace.isEncrypted()) {
                generateAndSaveEncryption(workspace.getId(), workspace.getPassword());
            }
        }else{
            
            if (workspace.isEncrypted()) {
                // TODO ask for the password!!
                PasswordDialog dialog = new PasswordDialog(new java.awt.Frame(), true);
                dialog.setVisible(true);
                String password = dialog.getPassword();
                workspace.setPassword(password);
                generateAndSaveEncryption(workspace.getId(), password);
            }
            
            // new workspace, let's create the workspace folder
            controller.createNewWorkspace(workspace);
            // save it in DB
            workspace.merge();
            localWorkspaces.put(workspace.getId(), workspace);
        }
        
    }
    
    private void generateAndSaveEncryption(String id, String password) throws InitializationException {
        try {
            // Create workspace encryption
            Encryption encryption = new Encryption(password);
            this.workspaceEncryption.put(id, encryption);
        } catch (ConfigException ex) {
            throw new InitializationException(ex);
        }
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
    
    private void getAndQueueChanges(CloneWorkspace workspace, ChangeManager changeManager) {
        // 3. Get changes and queue them
        List<Update> changes = server.getChanges(getAccountId(), workspace);
        changeManager.queueUpdates(changes);
    }
    
    public void addNewWorkspace(CloneWorkspace cloneWorkspace) throws Exception {
        
        if (cloneWorkspace.isEncrypted()) {
            Encryption encryption = new Encryption(cloneWorkspace.getPassword());
            this.workspaceEncryption.put(cloneWorkspace.getId(), encryption);
        }
        
        ChangeManager changeManager = remoteWatcher.getChangeManager();
        changeManager.start();
        
        try {
            // From now on, there will exist a new RemoteWorkspaceImpl which will be listen to the changes that are done in the SyncServer
            broker.bind(cloneWorkspace.getId().toString(), new RemoteWorkspaceImpl(cloneWorkspace, changeManager));
        } catch (Exception ex) {
            throw new Exception(ex);
        }
        
        // Get changes
        List<Update> changes = server.getChanges(getAccountId(), cloneWorkspace);
        changeManager.queueUpdates(changes);
        
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