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
package com.stacksync.desktop;

import com.stacksync.desktop.chunker.TTTD.RollingChecksum;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConnectionController;
import com.stacksync.desktop.config.ConnectionTester;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.gui.server.Desktop;
import com.stacksync.desktop.gui.settings.SettingsDialog;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.tray.TrayEventListenerImpl;
import com.stacksync.desktop.gui.wizard.WizardDialog;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.periodic.CacheCleaner;
import com.stacksync.desktop.periodic.TreeSearch;
import com.stacksync.desktop.repository.Uploader;
import com.stacksync.desktop.watch.local.LocalWatcher;
import com.stacksync.desktop.watch.remote.ChangeManager;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * Represents the application.
 *
 * <ul>
 * <li>{@link Watcher}: Listens to changes of the file system in the given local
 * sync folder. Passes changes to the indexer.
 * <li>{@link Indexer}: Reads local files and compares them to the versions in
 * local database. If necessary, it creates DB versions of new or altered files
 * and passes them to the storage manager for upload.
 * <li>{@link Uploader}: Uploads and downloads remote files from the shared
 * storage. Receives upload requests by the {@link Indexer}, and download
 * requests by the {@link PeriodicStorageMonitor}.
 * <li>{@link PeriodicStorageMonitor}: Checks the online storage for changes in
 * regular intervals, then downloads changes and notifies the
 * {@link ChangeManager}.
 * </ul>
 *
 * <p>General application To-Do list: Focus: <b>GET IT TO WORK!</b>
 * <ul>
 * <li>TODO [high] adjust separator for Win/Linux platforms: e.g. transform "\"
 * to "/" EVERYWHERE!
 * </ul>
 *
 * <p>Medium priority To-Do list:
 * <ul>
 * <li>TODO [medium] Connectivity management: Handle broken connections in every
 * single class
 * <li>TODO [medium] Make checksum long value instead of int, cp.
 * {@link RollingChecksum}
 * </ul>
 *
 * <p>Low priority To-Do list:
 * <ul>
 * <li>TODO [low] make platform specific file manager integration (windows
 * explorer, mac finder, ...)
 * <li>TODO [low] cache: implement a cache-cleaning functionality for the local
 * and online storage.
 * <li>TODO [low] cache: implement a cache-size parameter for the local cache.
 * </ul>
 *
 * <p>Wish list:
 * <ul>
 * <li>TODO [wish] strategy for down/uploading : FIFO, larget first, ...
 * </ul>
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Application implements ConnectionController, ApplicationController {

    private final Logger logger = Logger.getLogger(Application.class.getName());
    private final ResourceBundle resourceBundle = Config.getInstance().getResourceBundle();
    
    private Config config;
    private Desktop desktop;
    private Indexer indexer;
    private LocalWatcher localWatcher;
    private Tray tray;
    private TreeSearch periodic;
    private CacheCleaner cache;
    private SettingsDialog settingsDialog;
    private ConnectionTester connectionTester;
    private Profile profile;

    public Application() {
        this.connectionTester = new ConnectionTester(this);
    }

    public void start() throws InitializationException {  
        
        logger.info("Starting Application.");
        
        // Do NOT change the order of these method calls!
        // They strongly depend on each other.        
        initDependencies();
        boolean deamonMode = config.isDaemonMode();
        if (!deamonMode) {
            logger.info("Init UI...");
            initUI();
        }

        tray.setStartDemonOnly(deamonMode);
        
        boolean success = loadProfile();
        
        if (success) {
            startThreads();
        } else {
            
            this.tray.setStatusText("StackSync", resourceBundle.getString("tray_no_internet"));
            // Start process to check internet connection
            this.connectionTester.start();
        }
    }

    private void initDependencies() {
        logger.info("Instantiating dependencies ...");
        config = Config.getInstance();

        new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseHelper.getInstance().getEntityManager(); // creates de database to solve some time after.
            }
        }, "InitDatabase").start();


        desktop = Desktop.getInstance();
        indexer = Indexer.getInstance();
        localWatcher = LocalWatcher.getInstance();
        periodic = new TreeSearch();
        cache = new CacheCleaner();
        tray = Tray.getInstance();
        profile = config.getProfile();
        
        tray.registerProcess("StackSync");
    }
    
    private void startThreads() {
        // Start the rest
        // Desktop integration
        desktop.start(config.isDaemonMode()); // must be started before indexer!
        indexer.start();
        localWatcher.start();
        periodic.start();
        cache.start();
        RemoteLogs.getInstance().setActive(config.isRemoteLogs());
    }

    private void doShutdown() {
        logger.info("Shutting down ...");

        tray.destroy();
        indexer.stop();
        localWatcher.stop();
        periodic.stop();
        cache.stop();
        desktop.stop(config.isDaemonMode());

        if (config.getProfile() != null) {
            config.getProfile().stop();
        }

        System.exit(0);
    }

    private void initUI() throws InitializationException {
        
        /* THIS IS NOT NECESSARY SINCE THE SETTINGS DIALOG IS NOT ENABLED
         * LEAVE THE CODE HERE FOR A FUTURE USE!
         */
        
        // Settings Dialog
        /* try {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    settingsDialog = new SettingsDialog();

                    for (Profile p : config.getProfiles().list()) {
                        settingsDialog.addProfileToTree(p, false);
                    }
                }
            });
        } catch (Exception ex) {
            //checkthis
            logger.error("Unable to init SettingsDialog: ", ex);
            RemoteLogs.getInstance().sendLog(ex);
            throw new InitializationException(ex);
        }*/

        // Tray
        tray.init(resourceBundle.getString("tray_uptodate"));
        tray.addTrayEventListener(new TrayEventListenerImpl(this));
        tray.updateUI();

        /*
         // Desktop integration
         desktop.start(); // must be started before indexer!
        */
    }
    
    private boolean loadProfile() throws InitializationException {
        
        boolean success = true;
        Profile profile = config.getProfile();
        // a. Launch first time wizard                
        if (!profile.isInitialized()) {

            if (!config.isDaemonMode()) {
                profile = initFirstTimeWizard();
            } else {
                logger.error("Daemon needs config.xml have minimum one profile to start the application.");
            }

            if (profile == null) {
                doShutdown();
            }
        } else { // b. Activate profiles (Index files, then start local/remote watcher)
            try {
                initProfile();
            } catch (StorageConnectException ex) {
                logger.error(ex);
                success = false;
            }
        }
        
        return success;
    }

    private void initProfile() throws InitializationException, StorageConnectException {

        tray.setStatusIcon("StackSync", Tray.StatusIcon.UPDATING);
        tray.updateUI();    // This is only necessary in Linux...
        tray.setStatusText("StackSync", resourceBundle.getString("tray_initializing"));
        
        if (profile == null) {
            throw new InitializationException("No profile found!");
        }
        
        if (!profile.isEnabled()) {
            return;
        }

        profile.savePathToRegistry();

        try {
            profile.setActive(true);
        } catch (InitializationException ex) {
            logger.error("Can't load the profile.", ex);
            RemoteLogs.getInstance().sendLog(ex);
            throw ex;
        }
        
        tray.setStatusIcon("StackSync", Tray.StatusIcon.UPTODATE);
        tray.updateUI();    // This is only necessary in Linux...
        tray.setStatusText("StackSync", "");

    }

    private Profile initFirstTimeWizard() {
        Profile newProfile = WizardDialog.showWizardOpenJdk(true);

        // Ok clicked
        if (newProfile != null) {
            config.setProfile(newProfile);
            this.profile = newProfile;
            //settingsDialog.addProfileToTree(profile, false);
            tray.updateUI();

            newProfile.savePathToRegistry();
            try {
                config.save();
                newProfile.setActive(true);
            } catch (ConfigException ex) {
                logger.error("Could not save profile from first-start wizard. EXITING.", ex);
                RemoteLogs.getInstance().sendLog(ex);
                throw new RuntimeException("Could not save profile from first-start wizard. EXITING.", ex);
            } catch (InitializationException ex) {
                logger.error("Could not save profile from first-start wizard. EXITING.", ex);
                RemoteLogs.getInstance().sendLog(ex);
                throw new RuntimeException("Could not save profile from first-start wizard. EXITING.", ex);
            } catch (StorageConnectException ex) {
                // TODO is this possible???
                logger.error("Could not save profile from first-start wizard. EXITING.", ex);
                RemoteLogs.getInstance().sendLog(ex);
                throw new RuntimeException("Could not save profile from first-start wizard. EXITING.", ex);
            }
        }

        return newProfile;
    }

    @Override
    public void connectionEstablished() {
        logger.info("Connection established!!");
        
        this.connectionTester.stop();
        tray.setStatusText("StackSync", "");
        
        boolean success = false;
        try {
            success = loadProfile();
        } catch (InitializationException ex) {
            logger.error(ex);
            System.exit(2);
        }
        
        if (success) {
            startThreads();
        } else {
            this.tray.setStatusText("StackSync", resourceBundle.getString("tray_no_internet"));
            // Start process to check internet connection
            this.connectionTester.start();
        }
    }

    @Override
    public void pauseSync() {
        
        logger.info("Pausing syncing.");

        if (profile == null) {
            return;
        }
        
        try {
            profile.setActive(false);
        } catch (Exception ex) {
           logger.error("Could not pause synchronization: ", ex);
           RemoteLogs.getInstance().sendLog(ex);
           return;
        }
        
        indexer.stop();
        localWatcher.stop();
        periodic.stop();
        cache.stop();
        desktop.stop(config.isDaemonMode());

        tray.setStatusIcon("StackSync", Tray.StatusIcon.DISCONNECTED);
        tray.updateUI();    // This is only necessary in Linux...
        tray.setStatusText("StackSync", resourceBundle.getString("tray_paused_sync"));
    }

    @Override
    public void resumeSync() {
        
        logger.info("Resume syncing.");
        
        tray.setStatusIcon("StackSync", Tray.StatusIcon.UPDATING);
        tray.updateUI();    // This is only necessary in Linux...
        tray.setStatusText("StackSync", resourceBundle.getString("tray_initializing"));
        
        try {
            initProfile();
        } catch (InitializationException ex) {
            // Error logged in initProfiles function.
            return;
        } catch (StorageConnectException ex) {
            logger.error("Could not pause synchronization: ", ex);
            RemoteLogs.getInstance().sendLog(ex);
            return;
        }
        
        tray.setStatusIcon("StackSync", Tray.StatusIcon.UPTODATE);
        tray.updateUI();
        tray.setStatusText("StackSync", "");
        
        desktop.start(config.isDaemonMode()); // must be started before indexer!
        
        indexer.start();
        localWatcher.start();
        periodic.start();
        cache.start();
    }
    
    @Override
    public void doShutdownTray() {
        doShutdown();
    }
    
}
