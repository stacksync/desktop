/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.config.profile;

import com.stacksync.desktop.Environment;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.Repository;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.exceptions.StorageUnauthorizeException;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.repository.Uploader;
import com.stacksync.desktop.syncserver.RemoteWorkspaceImpl;
import com.stacksync.desktop.syncserver.Server;
import com.stacksync.desktop.util.WinRegistry;
import com.stacksync.desktop.watch.local.LocalWatcher;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.desktop.watch.remote.RemoteWatcher;
import omq.common.broker.Broker;

/**
 *
 * @author Philipp C. Heckel
 */
public class Profile implements Configurable {

    public static String tagName() {
        return "profile";
    }

    public static String xpath(int id) {
        return "profile[@id='" + id + "']";
    }
    private static final Logger logger = Logger.getLogger(Profile.class.getName());
    private static final LocalWatcher localWatcher = LocalWatcher.getInstance();
    private Environment env = Environment.getInstance();
    //private static final Config config = Config.getInstance();
    private boolean active;
    private boolean enabled;
    private boolean initialized;
    private String name;
    private String cloudId;
    private Repository repository;
    private Folder folder;
    private Uploader uploader;
    private RemoteWatcher remoteWatcher;
    private BrokerProperties brokerProps;
    private Broker broker;
    private Server server;

    public Profile() {
        active = false;
        initialized = false;
        
        enabled = true;
        name = "(unknown)";

    }
    
    private void initialize() {
        
        if (initialized){
            return;
        }
        
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
            File folder = getFolder().getLocalFile();
            if (!folder.exists()) {
                folder.mkdirs();
            }

            TransferManager transferManager = repository.getConnection().createTransferManager();
            try {
                transferManager.initStorage();
            } catch (StorageUnauthorizeException ex) {
                // Is this possible?? Password changed??
                throw new InitializationException(ex);
            } catch (StorageConnectException ex) {
                throw ex;
            } catch (StorageException ex) {
                throw new InitializationException(ex);
            }
            
            cloudId = transferManager.getUser();

            setFactory();
            server.updateDevice(cloudId);
            Map<Long, CloneWorkspace> workspaces = CloneWorkspace.InitializeWorkspaces(this);

            // Start threads 1/2
            uploader.start();
            ChangeManager changeManager = remoteWatcher.getChangeManager();
            changeManager.start();
                    
            for (CloneWorkspace w : workspaces.values()) {
                try {
                    // From now on, there will exist a new RemoteWorkspaceImpl which will be listen to the changes that are done in the SyncServer
                    broker.bind(w.getId().toString(), new RemoteWorkspaceImpl(w, changeManager));
                } catch (Exception ex) {
                    throw new InitializationException(ex);
                }

                // Get changes
                List<Update> changes = server.getChanges(cloudId, w);
                changeManager.queueUpdates(changes);
            }

            while (changeManager.queuesUpdatesIsWorking()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) { }
            }

            // Start threads 2/2            
            localWatcher.watch(this);

            remoteWatcher.setServer(server);
            remoteWatcher.start();
            //indexer.index(this); --> periodictree search do this.

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
    
    public String getCloudId() {
        return cloudId;
    }
    
    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
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

    @Override
    public void load(ConfigNode node) throws ConfigException {
        
        if (node == null) {
            return;
        }
        
        initialize();
        
        try {
            enabled = node.getBoolean("enabled");
            name = node.getProperty("name");
            cloudId = node.getProperty("cloudid");

            // Repo
            repository = new Repository();
            repository.load(node.findChildByName("repository"));

            // Folder
            folder = new Folder(this);
            folder.load(node.findChildByXPath("folder"));
            //folders = new Folders(this);
            //folders.load(node.findChildByXPath("folders"));

            // Remote IDs
            /*for (Folder folder : folders.list()) {
                repository.getAvailableRemoteIds().add(folder.getRemoteId());
            }*/

        } catch (Exception e) {
            throw new ConfigException("Unable to load profile: " + e, e);
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("enabled", enabled);
        node.setProperty("name", name);
        node.setProperty("cloudid", cloudId);

        // Repo
        repository.save(node.findOrCreateChildByXpath("repository", "repository"));

        // Folders
        folder.save(node.findOrCreateChildByXpath("folder", "folder"));

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