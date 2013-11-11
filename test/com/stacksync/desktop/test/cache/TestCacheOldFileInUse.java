/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.cache;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
import com.stacksync.desktop.periodic.CacheCleaner;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestCacheOldFileInUse {

    private static Config config = Config.getInstance(); 

    public static void main(String[] args) throws Exception {        
        Folder root = staticFunctionsTest.initConfig(config);
                
        CacheCleaner cache = new CacheCleaner();
                
        //create F1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");
        staticFunctionsTest.indexNewRequest(root, f1, null);
        
        File f2 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName2, "content2");
        CloneFile cf = staticFunctionsTest.indexNewRequest(root, f2, null);   
        
        cf.setSyncStatus(SyncStatus.SYNCING);
        //cf.setSyncStatus(SyncStatus.REMOTE);
        
        f2 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName3, "content2");
        cf = staticFunctionsTest.indexNewRequest(root, f2, null);  
                
        Calendar cal = Calendar.getInstance();  
        cal.set(Calendar.HOUR, cal.get(Calendar.HOUR) - 2); 
        Date dateNow = cal.getTime();    
        
        for(CloneChunk chunk: cf.getChunks()){
            File fileChunk = config.getCache().getCacheChunk(chunk);                        
            fileChunk.setLastModified(dateNow.getTime());
        }
        
        cache.start();
        
        while(true) { }
    }
}
