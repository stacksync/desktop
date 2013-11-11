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
package com.stacksync.desktop.index;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Application;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.index.requests.CheckIndexRequest;
import com.stacksync.desktop.index.requests.DeleteIndexRequest;
import com.stacksync.desktop.index.requests.IndexRequest;
import com.stacksync.desktop.index.requests.MoveIndexRequest;
import com.stacksync.desktop.index.requests.NewIndexRequest;
import com.stacksync.desktop.util.FileLister;

/**
 * Indexes new and changed files and adds corresponding database entries
 * if necessary. The indexer is mainly called by the {@link Watcher} inside the
 * {@link Application} object.
 *
 * <p>It mainly consists of a request queue and one worker thread that handles
 * events such as new, changed, renamed or deleted files or folders.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Indexer {
    private final Logger logger = Logger.getLogger(Indexer.class.getName());
    private final Config config = Config.getInstance();
    private final DatabaseHelper db = DatabaseHelper.getInstance();
        
    private static Indexer instance;
    
    private BlockingQueue<IndexRequest> queue;
    private Thread worker;
    private Tray tray = Tray.getInstance();

    public Indexer() {
        logger.info(config.getMachineName()+"#Creating indexer ...");
             
        this.queue = new LinkedBlockingQueue<IndexRequest>();
        this.worker = null; // cp. start()
    }
    
    public static synchronized Indexer getInstance() {
        if (instance == null) {
            instance = new Indexer();
        }
        
        return instance;
    }

    public synchronized void start() {
        // Already running!
        if (worker != null) {
            return;
        }
        
        // Start it
        logger.info(config.getMachineName()+"#Starting indexer thread ...");
        tray.registerProcess(this.getClass().getSimpleName());
        
        worker = new Thread(new IndexWorker(), "Indexer");
        worker.start();
    }

    public synchronized void stop() {
        if (worker == null) {
            return;
        }
        
        logger.info(config.getMachineName()+"#Stopping indexer thread ...");
        
        worker.interrupt();
        worker = null;
    }

    public void index(Profile profile) { 
        logger.debug(config.getMachineName()+"#Reading folders in profile "+profile.getName()+" ...");
                
        for (Folder folder : profile.getFolders().list()) {
            if (!folder.isActive() || folder.getLocalFile() == null) {
                continue;
            }
            logger.info(config.getMachineName()+"#- Folder "+folder.getLocalFile()+" ...");
            
            // Check for files that do NOT exist anymore
            List<CloneFile> dbFiles = db.getFiles(folder);
            
            for (CloneFile dbFile: dbFiles) {
                if (!dbFile.getFile().exists() && dbFile.getSyncStatus() != CloneFile.SyncStatus.REMOTE) {
                    logger.info(config.getMachineName()+"#File "+dbFile.getFile()+" does NOT exist anymore. Marking as deleted.");                    
                    queueDeleted(folder, dbFile.getFile());
                    //new DeleteIndexRequest(folder, dbFile).process();
                }
            }
                    
            // Check existing files
            new FileLister(folder.getLocalFile(), new FileListerListenerImpl(folder, this, true)).start();
        }	
        logger.debug(config.getMachineName()+"#Startup indexing of profile "+profile+" finished.");       
    }    
    
    /**
     * Check database to find matches for the given file. If no matches
     * or previous versions are found, the file is re-indexed completely.
     * 
     * @param file
     */
    public void queueChecked(Folder root, File file) {
        queue.add(new CheckIndexRequest(root, file));
    }

    /**
     * Adjusts the entry of a file that has been moved.
     * @param fromFile
     * @param toFile
     */
    public void queueMoved(Folder fromRoot, File fromFile, Folder toRoot, File toFile) {
        queue.add(new MoveIndexRequest(fromRoot, fromFile, toRoot, toFile));
    }
    
    public void queueMoved(CloneFile guessedPreviousVersion, Folder toRoot, File toFile) {
        queue.add(new MoveIndexRequest(guessedPreviousVersion, toRoot, toFile));
    }    

    public void queueDeleted(Folder root, File file) {
        queue.add(new DeleteIndexRequest(root, file));
    }
    
    public void queueDeleted(Folder root, CloneFile file) {
        queue.add(new DeleteIndexRequest(root, file));
    }    
    
    public void queueNewIndex(Folder root, File file, CloneFile previousVersion, long checksum){
        queue.add(new NewIndexRequest(root, file, previousVersion, checksum));
    }
    
    public void queueNewIndex(NewIndexRequest newRequest) {
        queue.add(newRequest);
    }

    private class IndexWorker implements Runnable {
        @Override
        public void run() {
            try {
                IndexRequest req;
                
                while (null != (req = queue.take())) {
                    tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "Indexing " + (queue.size() + 1) +  " files...");
                    
                    logger.info("Processing request "+req+".");      
                    req.process();
                    
                    if(queue.isEmpty()){
                        tray.setStatusText(this.getClass().getDeclaringClass().getSimpleName(), "");
                    }                    
                }
            } catch (InterruptedException ex) {
               logger.error(config.getMachineName()+"#Indexer interrupted. EXITING.", ex);
            }
        }
    }
}
