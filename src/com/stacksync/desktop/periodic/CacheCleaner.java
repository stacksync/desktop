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
package com.stacksync.desktop.periodic;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneChunk.CacheStatus;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;

/**
 * Cleans the local cache by deleting unused files.
 *
 * TODO Implement this.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CacheCleaner {

    private final Logger logger = Logger.getLogger(CacheCleaner.class.getName());
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Config config = Config.getInstance();
    private Timer timer;

    public CacheCleaner() {
        timer = null;
    }

    public synchronized void start() {        
        timer = new Timer("PeriodicCacheCleaner");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doPeriodicCheck();
            }
        }, 0, Constants.PERIODIC_CACHE_INTERVAL);
        
    }

    public synchronized void stop() {       
        if (timer == null) {
            return;
        }
        
        timer.cancel();
        timer = null;
    }
    
    
    private void doPeriodicCheck(){
        logger.debug(config.getMachineName()+"#Started periodic cache search ...");
            
        Calendar cal = Calendar.getInstance();  
        cal.set(Calendar.HOUR, cal.get(Calendar.HOUR) - 1); 
        Date dateNow = cal.getTime();        
        
        for (CloneChunk chunk: db.getChunkCached()) {
            logger.debug(config.getMachineName()+"#Trying to clean -> " + chunk + " ...");
            File chunkCacheFile = config.getCache().getCacheChunk(chunk);
            
            boolean canDelete = true;
            
            long lastModified = chunkCacheFile.lastModified();            
            if(lastModified > dateNow.getTime()){
                canDelete = false;
                logger.debug(config.getMachineName()+"#Chunk is newest lastmodified:" + lastModified + " > " + "dateNow:" + dateNow.getTime() + " ...");
            }

            if(canDelete){
                List<CloneFile> cloneFiles = db.getCloneFiles(chunk);
                
                for(CloneFile cf: cloneFiles){
                    if(cf.getSyncStatus() != SyncStatus.UPTODATE){
                        canDelete = false;
                        logger.debug(config.getMachineName()+"#Chunk is used by " + cf + " ...");
                        break;
                    }
                }
            }
            
            if(canDelete){
                chunkCacheFile.delete();
                chunk.setCacheStatus(CacheStatus.REMOTE);
                chunk.merge();
                logger.debug(config.getMachineName()+"#Deleting the chunk -> " + chunk + " ...");
            }            
        }
        logger.debug(config.getMachineName()+"#Finished periodic cache search. Now sleeping "+Constants.PERIODIC_CACHE_INTERVAL+" seconds.");
    }
    
}
