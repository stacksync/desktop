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
package com.stacksync.desktop.watch.remote;

import com.stacksync.syncservice.models.ObjectMetadata;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.exceptions.CouldNotApplyUpdateException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.syncserver.Server;
import com.stacksync.desktop.util.StringUtil;

/**
 * Does periodical checks on the online storage, and applies them locally.
 *
 * <p>This currently includes the following steps: <ul> <li>List files, identify
 * available update files <li>Download required update files <li>Identify
 * conflicts and apply updates/changes <li>Create local update files <li>Upload
 * local update file <li>Delete old update files online </ul>
 *
 * Unlike the {@link StorageManager}, this class uses its own
 * {@link TransferManager} to be able to synchronously wait for the storage.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class RemoteWatcher {
    
    private final Logger logger = Logger.getLogger(RemoteWatcher.class.getName());    
    
    private final int INTERVAL = 5000;
    private final int QUEUEPENDINGINTERVAL = 12;
    private int queuependinginterval = 60;
    
    private Config config;
    private DatabaseHelper db;
    private Profile profile;
    private ChangeManager changeManager;
    private Timer timer;
    private TransferManager transfer;
    
    private Server server;

    public RemoteWatcher(Profile profile) {
        this.profile = profile;
        this.changeManager = new ChangeManager(profile);
        this.timer = null;

        // cp. start()
        this.config = null;
        this.db = null;

        // cp. doUpdateCheck()
        this.transfer = null;
    }

    public synchronized void start() {
        // Dependencies
        if (config == null) {
            config = Config.getInstance();
            db = DatabaseHelper.getInstance();
        }

        // Reset connection
        reset();

        timer = new Timer("RemoteWatcher");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doUpdateCheck();
            }
        }, 0, INTERVAL);
    }

    public synchronized void stop() {
        if (timer == null) {
            return;
        }                
        
        changeManager.stop();
        timer.cancel();
        timer = null;
    }   

    private void reset() {
        if(transfer == null){
            transfer = profile.getRepository().getConnection().createTransferManager();                        
        }       
    }
    
    public ChangeManager getChangeManager() {
        return changeManager;
    }
    
    public void setServer(Server server){
        this.server = server;
    }
    
    private void doUpdateCheck() {
        logger.debug(config.getMachineName() + "#STARTING PERIODIC UPDATE CHECK ...");
        reset();

        try {            
            if(queuependinginterval >= QUEUEPENDINGINTERVAL*(INTERVAL/1000)){
                profile.getUploader().queuePendingFiles();                
                queuependinginterval = 0;
            }else{
                queuependinginterval++;
            }

            // 1. download update files
            // 3. Analyzing updates & looking for conflicts
            // 4. Create and upload local updates ///////
            commitLocalUpdateFile();

        } catch (StorageException ex) {
            logger.warn(config.getMachineName() + "#Update check failed. Trying again in a couple of seconds.", ex);
        } finally {
            logger.debug(config.getMachineName() + "#DONE WITH PERIODIC UPDATE CHECK ...");

            try {
                transfer.disconnect();
            } catch (StorageException ex) {
                logger.error("Can't disconnect the remote storage", ex);
                RemoteLogs.getInstance().sendLog(ex);
                /* Fressen! */
            }
        }
    }
    
    private void commitLocalUpdateFile() throws StorageException {
        Date lastUpdateFileDate = new Date();
        
        // Check if new update file needs to be created/uploaded
        Long fileVersionCount = db.getFileVersionCount(profile);
        if (fileVersionCount == 0) {
            logger.debug("No local changes. Skipping step upload.");
            return;
        }

        try {
            logger.info("Commit new changes.");
            
            Map<String, List<CloneFile>> updatedFiles = db.getHistoryUptoDate(profile);

            // Upload
            Workspace workspace = null;
            List<ObjectMetadata> commitObjects = new ArrayList<ObjectMetadata>();
            
            for(List<CloneFile> w: updatedFiles.values()){
                for(CloneFile c: w){
                    //c.setServerUploaded(true);
                    c.setServerUploadedTime(lastUpdateFileDate);
                    c.merge(); 
                    workspace = c.getWorkspace();
                    
                    ObjectMetadata obj = StringUtil.parseJson2Update(c);
                    commitObjects.add(obj);
                }
            }
            
            //String message = FileUtils.readFileToString(localUpdateFile);
            String cloudId = transfer.getUser();
            
            server.commit(cloudId, workspace, commitObjects);
        } catch (IOException ex) {
            logger.error("Failed to write file.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
    }

    public void restoreVersion(CloneFile restoringVersion) throws CouldNotApplyUpdateException {
        changeManager.restoreVersion(restoringVersion);
    }
}