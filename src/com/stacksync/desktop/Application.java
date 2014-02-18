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

import java.awt.EventQueue;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.server.Desktop;
import com.stacksync.desktop.gui.settings.SettingsDialog;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.wizard.WizardDialog;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.chunker.TTTD.RollingChecksum;
import com.stacksync.desktop.config.ConnectionController;
import com.stacksync.desktop.config.ConnectionTester;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.gui.tray.TrayEventListenerImpl;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.periodic.CacheCleaner;
import com.stacksync.desktop.periodic.TreeSearch;
import com.stacksync.desktop.repository.Uploader;
import com.stacksync.desktop.watch.local.LocalWatcher;
import com.stacksync.desktop.watch.remote.ChangeManager;
import java.util.ResourceBundle;

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
        // Desktop integration
        if (config.isServiceEnabled()) {
            desktop.start(deamonMode); // must be started before indexer!
        }
        
        boolean success = loadProfiles();
        
        
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
                config.getDatabase().getEntityManager(); // creates de database to solve some time after.
            }
        }, "InitDatabase").start();


        desktop = Desktop.getInstance();
        indexer = Indexer.getInstance();
        localWatcher = LocalWatcher.getInstance();
        //periodic = new TreeSearch();
        cache = new CacheCleaner();
        tray = Tray.getInstance();
        
        tray.registerProcess("StackSync");
    }
    
    private void startThreads() {
        // Start the rest
        indexer.start();
        localWatcher.start();
        //periodic.start();
        cache.start();
        RemoteLogs.getInstance().setActive(config.isRemoteLogs());
    }

    private void doShutdown() {
        logger.info("Shutting down ...");

        tray.destroy();
        indexer.stop();
        localWatcher.stop();
        //periodic.stop();
        cache.stop();

        for (Profile profile : config.getProfiles().list()) {
            profile.stop();
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
         if (config.isServiceEnabled()) {
         desktop.start(); // must be started before indexer!
         } */
    }
    
    private boolean loadProfiles() throws InitializationException {
        
        boolean success = true;
        // a. Launch first time wizard                
        if (config.getProfiles().list().isEmpty()) {

            Profile profile = null;
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
                initProfiles();
            } catch (StorageConnectException ex) {
                logger.error(ex);
                success = false;
            }
        }
        
        return success;
    }

    private void initProfiles() throws InitializationException, StorageConnectException {

        for (Profile profile : config.getProfiles().list()) {
            if (!profile.isEnabled()) {
                continue;
            }
            
            profile.savePathToRegistry();

            try {
                profile.setActive(true);
            } catch (InitializationException ex) {
                logger.error("Can't load the profile.", ex);
                RemoteLogs.getInstance().sendLog(ex);
                throw ex;
            }           
        }

    }

    private Profile initFirstTimeWizard() {
        Profile profile = WizardDialog.showWizardOpenJdk(true);

        // Ok clicked
        if (profile != null) {
            config.getProfiles().add(profile);
            //settingsDialog.addProfileToTree(profile, false);
            tray.updateUI();

            profile.savePathToRegistry();
            try {
                config.save();
                profile.setActive(true);
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

        return profile;
    }

    @Override
    public void connectionEstablished() {
        logger.info("Connection established!!");
        
        this.connectionTester.stop();
        tray.setStatusText("StackSync", "");
        
        boolean success = false;
        try {
            success = loadProfiles();
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

        for (Profile profile : config.getProfiles().list()) {
            try {
                profile.setActive(false);
            } catch (Exception ex) {
               logger.error("Could not pause synchronization: ", ex);
               RemoteLogs.getInstance().sendLog(ex);
               return;
            }
        }
        
        indexer.stop();
        localWatcher.stop();
        //periodic.stop();
        cache.stop();
        desktop.stop(config.isDaemonMode());

        tray.setStatusIcon("StackSync", Tray.StatusIcon.DISCONNECTED);
        tray.updateUI();    // This is only necessary in Linux...
        tray.setStatusText("StackSync", resourceBundle.getString("tray_paused_sync"));
    }

    @Override
    public void resumeSync() {
        
        logger.info("Resume syncing.");
        
        try {
            initProfiles();
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
        
        if (config.isServiceEnabled()) {
            desktop.start(config.isDaemonMode()); // must be started before indexer!
        }
        
        indexer.start();
        localWatcher.start();
        //periodic.start();
        cache.start();
    }
    
    @Override
    public void doShutdownTray() {
        doShutdown();
    }
    
}
