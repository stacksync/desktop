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
import java.io.File;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.server.Desktop;
import com.stacksync.desktop.gui.settings.SettingsDialog;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.tray.TrayEvent;
import com.stacksync.desktop.gui.tray.TrayEventListener;
import com.stacksync.desktop.gui.wizard.WizardDialog;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.chunker.TTTD.RollingChecksum;
import com.stacksync.desktop.config.ConnectionController;
import com.stacksync.desktop.config.ConnectionTester;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.periodic.CacheCleaner;
import com.stacksync.desktop.periodic.TreeSearch;
import com.stacksync.desktop.repository.Uploader;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.util.WinRegistry;
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
public class Application implements ConnectionController {

    private final Logger logger = Logger.getLogger(Application.class.getName());
    private final ResourceBundle resourceBundle = Config.getInstance().getResourceBundle();
    private final Environment env = Environment.getInstance();
    private Boolean startDemonOnly;
    private Config config;
    private Desktop desktop;
    private Indexer indexer;
    private LocalWatcher localWatcher;
    private Tray tray;
    private TreeSearch periodic;
    private CacheCleaner cache;
    private SettingsDialog settingsDialog;
    private ConnectionTester connectionTester;

    public Application(Boolean startDemonOnly) {
        this.startDemonOnly = startDemonOnly;
        this.connectionTester = new ConnectionTester(this);
    }

    public void start() throws InitializationException {  
        logger.info(env.getMachineName() + "#Starting Application daemon: " + startDemonOnly + " ...");

        
        // Do NOT change the order of these method calls!
        // They strongly depend on each other.        
        initDependencies();
        if (!startDemonOnly) {
            logger.info(env.getMachineName() + "#Init UI...");
            initUI();
        }

        tray.setStartDemonOnly(startDemonOnly);
        // Desktop integration
        if (config.isServiceEnabled()) {
            desktop.start(startDemonOnly); // must be started before indexer!
        }
        
        boolean success = loadProfiles();
        
        
        if (success) {
            startThreads();
        } else {
            
            this.tray.registerProcess("StackSync");
            this.tray.setStatusText("StackSync", "No Internet connection.");
            // Start process to check internet connection
            this.connectionTester.start();
        }
    }

    private void initDependencies() {
        logger.info(env.getMachineName() + "#Instantiating dependencies ...");
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
        tray = Tray.getInstance();
        periodic = new TreeSearch();
        cache = new CacheCleaner();
    }
    
    private void startThreads() {
        // Start the rest
        indexer.start();
        localWatcher.start();
        periodic.start();
        cache.start();
    }

    private void doShutdown() {
        logger.info(env.getMachineName() + "#Shutting down ...");

        tray.destroy();
        indexer.stop();
        localWatcher.stop();
        periodic.stop();
        cache.stop();

        for (Profile profile : config.getProfiles().list()) {
            profile.stop();
        }

        System.exit(0);
    }

    private void initUI() throws InitializationException {
        // Settings Dialog
        try {
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
        }

        // Tray
        tray.init(resourceBundle.getString("tray_uptodate"));
        tray.addTrayEventListener(new TrayEventListenerImpl());
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
            if (!startDemonOnly) {
                profile = initFirstTimeWizard();
            } else {
                logger.error(env.getMachineName() + "#Daemon needs config.xml have minimum one profile to start the application...");
            }

            if (profile == null) {
                doShutdown();
            }
        } else { // b. Activate profiles (Index files, then start local/remote watcher)
            try {
                initProfiles();
            } catch (StorageConnectException ex) {
                logger.error(desktop);
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
            
            this.savePathToRegistry(profile);

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
            settingsDialog.addProfileToTree(profile, false);
            tray.updateUI();

            this.savePathToRegistry(profile);
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

    private void WriteWindowsRegistry(Profile profile) throws Exception {

        String localPath = profile.getFolders().get("stacksync").getLocalFile().getPath();

        WinRegistry.writeStringValue(
                WinRegistry.HKEY_CURRENT_USER,
                "SOFTWARE\\StackSync",
                "FilterFolder",
                localPath);

        WinRegistry.writeStringValue(
                WinRegistry.HKEY_CURRENT_USER,
                "SOFTWARE\\StackSync",
                "EnableOverlay",
                "1");
    }
    
    private void savePathToRegistry(Profile profile){
        if (env.getOperatingSystem() == Environment.OperatingSystem.Windows) {
            try {
                WriteWindowsRegistry(profile);
            } catch (Exception ex) {
                logger.error("Could not write Windows registry", ex);
            }
        }
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
            // TODO change language
            this.tray.setStatusText("StackSync", "No Internet connection.");
            // Start process to check internet connection
            this.connectionTester.start();
        }
    }

    private class TrayEventListenerImpl implements TrayEventListener {

        @Override
        public void trayEventOccurred(TrayEvent event) {
            switch (event.getType()) {
                case OPEN_FOLDER:
                    File folder = new File((String) event.getArgs().get(0));
                    FileUtil.openFile(folder);
                    break;

                case PREFERENCES:
                    settingsDialog.setVisible(true);
                    break;

                case WEBSITE:
                    FileUtil.browsePage(Constants.APPLICATION_URL);
                    break;

                case WEBSITE2:
                    FileUtil.browsePage(Constants.APPLICATION_URL2);
                    break;

                case QUIT:
                    doShutdown();
                    break;

                default:
                    //checkthis
                    logger.warn(env.getMachineName() + "#Unknown tray event type: " + event);
                // Fressen.
            }
        }
    }
}
