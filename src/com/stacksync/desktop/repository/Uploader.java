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

import com.stacksync.desktop.Constants;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.exceptions.StorageQuotaExcedeedException;
import com.stacksync.desktop.gui.server.Desktop;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.repository.files.RemoteFile;
import java.util.ArrayList;

/**
 * Represents the remote storage. Processes upload and download requests
 * asynchonously.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Uploader {

    private static final int MAX_RETRIES = 3;
    private final Config config = Config.getInstance();
    private final Logger logger = Logger.getLogger(Uploader.class.getName());
    private final Tray tray = Tray.getInstance();
    private final Desktop desktop = Desktop.getInstance();
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    
    private Profile profile;
    private TransferManager transfer;
    private BlockingQueue<CloneFile> queue;
    private Thread worker;
    private CloneFile workingFile;

    public Uploader(Profile profile) {
        this.profile = profile;
        this.queue = new LinkedBlockingQueue<CloneFile>();
        
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

    private void queuePendingList(List<CloneFile> filesSyncing){
        for (CloneFile file: filesSyncing) {

            boolean found = false;
            for (CloneFile file2 : queue) {
                if (file.getName().compareTo(file2.getName()) == 0
                        && file.getChecksum() == file2.getChecksum()
                        && file.getId() == file2.getId()
                        && file.getVersion() == file2.getVersion()
                        && file.getSize() == file2.getSize()) {

                    found = true;
                    break;
                }
            }

            if (!found) {
                queue(file);
            }
        }
    }
    
    public synchronized void queuePendingFiles() {
        //Queue the pending Syncing and Local files if networks goes down and after ups.
        List<CloneFile> filesSyncing = db.getFiles(profile.getFolder(), CloneFile.SyncStatus.SYNCING);
        queuePendingList(filesSyncing);
        
        filesSyncing = db.getFiles(profile.getFolder(), CloneFile.SyncStatus.LOCAL);
        queuePendingList(filesSyncing);
    }

    public synchronized void stop() {
        if (worker == null || worker.isInterrupted()) {
            return;
        }

        worker.interrupt();
        worker = null;
    }

    private void searchAddInQueue(CloneFile file) throws InterruptedException{
        boolean found = false;
        for(CloneFile cf: queue){
            if(cf.getId() == file.getId() && cf.getVersion() == file.getVersion()){                        
                found = true;
                break;
            }
        }

        if(!found){            
            queue.put(file);
        }        
    }
    
    public synchronized void queue(CloneFile file) {
        try {   
            if(workingFile == null){                
                searchAddInQueue(file);
            } else{            
                if(workingFile.getId() != file.getId() || workingFile.getVersion() != file.getVersion()){
                    searchAddInQueue(file);
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
                        if(!workingFile.isFolder()){
                            processRequest(workingFile);
                        }
                    } catch (Exception ex) {
                        workingFile.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
                        workingFile.merge();
                        logger.error(ex);
                    }
                    
                    workingFile = null;
                    if (queue.isEmpty()) {
                        tray.setStatusIcon(this.getClass().getDeclaringClass().getSimpleName(), Tray.StatusIcon.UPTODATE);
                        tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "");
                    }
                }
            } catch (InterruptedException iex) { }
        }

        private void processRequest(CloneFile file) throws Exception {            
            logger.info("UploadManager: Uploading file " + file.getFileName() + " ...");           

            // Update DB sync status                
            //now do this the newIndexRequest
            file = db.getFileOrFolder(file.getId(), file.getVersion());
            file.setSyncStatus(CloneFile.SyncStatus.SYNCING);
            file.merge();

            touch(file, SyncStatus.SYNCING);
            
            CloneFile oldVersion = file.getLastSyncedVersion();
            List<CloneChunk> oldChunks = null;
            if (oldVersion != null) {
                oldChunks = oldVersion.getChunks();
            }

            int numChunk = 0;
            List<CloneChunk> uploadedChunks = new ArrayList<CloneChunk>();
            for (CloneChunk chunk: file.getChunks()) {

                if(numChunk % 10 == 0){
                    tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "Uploading " + (queue.size() + 1) +  " files...");
                }
                
                // Chunk has been uploaded before
                if (oldChunks != null && oldChunks.contains(chunk)) {
                    continue;
                }

                // Upload it!
                try {
                    logger.info("UploadManager: Uploading chunk (" + numChunk + File.separator + file.getChunks().size() + ") " + chunk.getName() + " ...");
                    uploadChunk(file, chunk);
                    uploadedChunks.add(chunk);
                } catch (Exception ex){
                    removeChunks(uploadedChunks, file.getWorkspace());
                    throw ex;
                }
                
                numChunk++;
            }
            logger.info("UploadManager: File " + file.getAbsolutePath() + " uploaded");
            
            // Remove chunks from previous version
            if (oldChunks != null && oldVersion != null) {
                List<CloneChunk> newChunks = file.getChunks();
                oldChunks.removeAll(newChunks);
                removeChunks(oldChunks, oldVersion.getWorkspace());
            }
            
            // Update DB sync status
            file.setSyncStatus(SyncStatus.UPTODATE);
            file.merge();

            touch(file, SyncStatus.UPTODATE);
        }
        
        private void uploadChunk(CloneFile file, CloneChunk chunk ) throws StorageException, StorageQuotaExcedeedException {
            
            String fileRemoteName = chunk.getName();
            int retry = 0;
            boolean completed = false;
            
            while (!completed && retry < MAX_RETRIES) {
                
                try {
                    CloneWorkspace workspace = file.getWorkspace();
                    transfer.upload(config.getCache().getCacheChunk(chunk), new RemoteFile(fileRemoteName), workspace);
                    completed = true;
                } catch (StorageException ex) {
                    logger.warn("UploadManager: Uploading chunk "+chunk.getName() + " FAILED!!", ex);
                    if (++retry == MAX_RETRIES){
                        logger.error("Storage Exception after 3 retries: ", ex);
                        throw ex;
                    }
                } catch (StorageQuotaExcedeedException ex) {
                    logger.warn("UploaderManager: Quota excedeed.", ex);
                    if (++retry == MAX_RETRIES) { 
                        File imageFile = new File(config.getResDir() + File.separator + "logo48.png");
                        tray.notify(Constants.APPLICATION_NAME, "Quota exceeded", imageFile);
                        throw ex;
                    }
                }
            }
        }
        
        private void removeChunks(List<CloneChunk> chunks, CloneWorkspace workspace) throws StorageException {
            for(CloneChunk chunk : chunks) {
                transfer.delete(new RemoteFile(chunk.getName()), workspace);
            }
        }

        private void touch(CloneFile file, SyncStatus syncStatus) {
            // Touch myself
            desktop.touch(file.getFile());

            // Touch parents
            CloneFile childCF = file;
            CloneFile parentCF;

            while (null != (parentCF = childCF.getParent())) {
                if (parentCF.getSyncStatus() != syncStatus) {
                    //parentCF.setSyncStatus(syncStatus);
                    //parentCF.merge();

                    desktop.touch(parentCF.getFile());
                }

                childCF = parentCF;
            }
        }       
    }
}