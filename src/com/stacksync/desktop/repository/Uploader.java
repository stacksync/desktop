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
package com.stacksync.desktop.repository;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneItemVersion.SyncStatus;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.exceptions.StorageQuotaExcedeedException;
import com.stacksync.desktop.gui.server.Desktop;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.repository.files.RemoteFile;

/**
 * Represents the remote storage. Processes upload and download requests
 * asynchonously.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Uploader {

    private final int CACHE_FILE_LIST = 60000;
    private final Config config = Config.getInstance();
    private final Logger logger = Logger.getLogger(Uploader.class.getName());
    private final Tray tray = Tray.getInstance();
    private final Desktop desktop = Desktop.getInstance();
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Environment env = Environment.getInstance();
    
    private Profile profile;
    private TransferManager transfer;
    private BlockingQueue<CloneItemVersion> queue;
    private Thread worker;
    private Map<String, RemoteFile> fileList;
    private Date cacheLastUpdate;
    private CloneItemVersion workingFile;

    public Uploader(Profile profile) {
        this.profile = profile;
        this.queue = new LinkedBlockingQueue<CloneItemVersion>();
        
        this.tray.registerProcess(this.getClass().getSimpleName());
        this.worker = null; // cmp. method 'start'
        workingFile = null;
    }

    public synchronized void start() {
        if (worker != null) {
            return;
        }

        tray.registerProcess(this.getClass().getSimpleName());
        
        transfer = profile.getRepository().getConnection().createTransferManager();
        queuePendingFiles();

        worker = new Thread(new Worker(), "UploaderWorker");
        worker.start();
    }

    private void queuePendingList(List<CloneItemVersion> filesSyncing){
        for (CloneItemVersion version: filesSyncing) {

            boolean found = false;
            for (CloneItemVersion version2 : queue) {
                if (version.getChecksum() == version2.getChecksum()
                        && version.getVersion() == version2.getVersion()
                        && version.getSize() == version2.getSize()
                        && version.getItem().getId().equals(version2.getItem().getId())
                        && version.getItem().getName().equals(version2.getItem().getName())) {

                    found = true;
                    break;
                }
            }

            if (!found) {
                queue(version);
            }
        }
    }
    
    public synchronized void queuePendingFiles() {
        //Queue the pending Syncing and Local files if networks goes down and after ups.
        List<CloneItemVersion> filesSyncing = db.getFiles(SyncStatus.SYNCING);
        queuePendingList(filesSyncing);
        
        filesSyncing = db.getFiles(SyncStatus.LOCAL);
        queuePendingList(filesSyncing);
    }

    public synchronized void stop() {
        if (worker == null || worker.isInterrupted()) {
            return;
        }

        worker.interrupt();
        worker = null;
    }

    private void searchAddInQueue(CloneItemVersion version) throws InterruptedException{
        boolean found = false;
        for(CloneItemVersion version2: queue){
            if(version2.getVersion() == version.getVersion() && version2.getItem().getId().equals(version.getItem().getId())){                        
                found = true;
                break;
            }
        }

        if(!found){            
            queue.put(version);
        }        
    }
    
    public synchronized void queue(CloneItemVersion version) {
        try {   
            if(workingFile == null){                
                searchAddInQueue(version);
            } else{            
                if(workingFile.getVersion() != version.getVersion() || !workingFile.getItem().getId().equals(version.getItem().getId())){
                    searchAddInQueue(version);
                }                
            }
        } catch (InterruptedException ex) {
            logger.error("Exception: ", ex);
        }            
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            try {                
                workingFile = null;                
                while (null != (workingFile = queue.take())) {
                    tray.setStatusIcon(this.getClass().getDeclaringClass().getSimpleName(), Tray.StatusIcon.UPDATING);
                    tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "Uploading " + (queue.size() + 1) +  " files...");
                    
                    try {
                        if(!workingFile.getItem().isFolder()){
                            processRequest(workingFile);
                        } else{
                            logger.info("Exception folder doens't have chunks!!!");
                        }
                    } catch (StorageException ex) {
                        logger.error("Could not process the file: ", ex);
                        RemoteLogs.getInstance().sendLog(ex);
                        
                        workingFile.setSyncStatus(SyncStatus.SYNCING);
                        workingFile.merge();
                        queue(workingFile);
                    } catch (NullPointerException ex1){
                        logger.info("Could not process the file ->", ex1);
                    } catch (StorageQuotaExcedeedException ex) {
                        logger.info("Stop syncing file due to over quota.");
                        
                        workingFile.setSyncStatus(SyncStatus.UNSYNC);
                        workingFile.merge();
                    }
                    
                    workingFile = null;
                    if (queue.isEmpty()) {
                        tray.setStatusIcon(this.getClass().getDeclaringClass().getSimpleName(), Tray.StatusIcon.UPTODATE);
                        tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "");
                    }
                }
            } catch (InterruptedException iex) {
                logger.error("Exception ", iex);
            }
        }

        private void processRequest(CloneItemVersion file) throws StorageException, StorageQuotaExcedeedException {            
            logger.info("UploadManager: Uploading file " + file.getFileName() + " ...");           

            // Update DB sync status                
            //now do this the newIndexRequest
            //file = db.getFileOrFolder(file.getId(), file.getVersion());
            file.setSyncStatus(SyncStatus.SYNCING);
            file.merge();

            touch(file.getItem(), SyncStatus.SYNCING);

            // TODO IMPORTANT What about the DB to check the cunks!!!??
            // Get file list (to check if chunks already exist)
            /*if (cacheLastUpdate == null || fileList == null || System.currentTimeMillis()-cacheLastUpdate.getTime() > CACHE_FILE_LIST) {                
                try {
                    fileList = transfer.list();
                } catch (StorageException ex) {
                    logger.error("UploadManager: List FAILED!!", ex);
                    RemoteLogs.getInstance().sendLog(ex);
                }
            }*/

            int numChunk = 0;
            for (CloneChunk chunk: file.getChunks()) {
                // Chunk has been uploaded before

                if(numChunk % 10 == 0){
                    tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "Uploading " + (queue.size() + 1) +  " files...");
                }
                
                String fileRemoteName = chunk.getFileName();
                RemoteFile rFile = getRemoteFile(fileRemoteName);
                if (rFile != null) {
                    File localFile = config.getCache().getCacheChunk(chunk);

                    long localSize = localFile.length();
                    long remoteSize = rFile.getSize();

                    if (localSize == remoteSize) {
                        logger.info("UploadManager: Chunk (" + numChunk + File.separator + file.getChunks().size() + ") " + chunk.getFileName() + " already uploaded");              
                        continue;
                    }
                }

                // Upload it!
                try {
                    logger.info("UploadManager: Uploading chunk (" + numChunk + File.separator + file.getChunks().size() + ") " + chunk.getFileName() + " ...");
                    
                    CloneWorkspace workspace = file.getItem().getWorkspace();
                    transfer.upload(config.getCache().getCacheChunk(chunk), new RemoteFile(fileRemoteName), workspace);
                } catch (StorageException ex) {
                    logger.error("UploadManager: Uploading chunk ("+ numChunk +File.separator+file.getChunks().size()+") "+chunk.getFileName() + " FAILED!!", ex);
                    throw ex;
                } catch (StorageQuotaExcedeedException ex) {
                    logger.error("UploaderManager: Quota excedeed.", ex);
                    throw ex;
                }
                
                numChunk++;
            }
            logger.info("UploadManager: File " + file.getItem().getAbsolutePath() + " uploaded");

            /**
             * Test this code:
            file.setSyncStatus(SyncStatus.UPTODATE);
            file.merge();
             * Is it necessary to get again the file from the DB???
             */
            
            //file = db.getFileOrFolder(file.getId(), file.getVersion());
            
            // Update DB sync status
            file.setSyncStatus(SyncStatus.UPTODATE);
            file.merge();

            touch(file.getItem(), SyncStatus.UPTODATE);
        }

        private RemoteFile getRemoteFile(String fileRemoteName){
            
            RemoteFile rFile = null;
            if(fileList != null){
                if (fileList.containsKey(fileRemoteName)) {
                    rFile = fileList.get(fileRemoteName);                                                       
                } else if (fileList.containsKey(fileRemoteName.substring(1))) {
                    rFile = fileList.get(fileRemoteName.substring(1));                                                                         
                }
            }

            return rFile;
        }

        private void touch(CloneItem file, SyncStatus syncStatus) {
            // Touch myself
            desktop.touch(file.getFile());

            // Touch parents
            CloneItem childCF = file;
            CloneItem parentCF;

            while (null != (parentCF = childCF.getParent())) {
                
                CloneItemVersion latestVersion = parentCF.getLatestVersion();
                if (latestVersion != null && latestVersion.getSyncStatus() != syncStatus) {
                    //parentCF.setSyncStatus(syncStatus);
                    //parentCF.merge();

                    desktop.touch(parentCF.getFile());
                }

                childCF = parentCF;
            }
        }       
    }
}