/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
import com.stacksync.desktop.index.requests.DeleteIndexRequest;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.test.staticFunctionsTest;
import com.stacksync.desktop.watch.remote.ChangeManager;

/**
 *
 * @author gguerrero
 */
public class TestConflictedDeleteWithChange {

    private static Config config = Config.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance();
       
    public static void main(String[] args) throws Exception {
        Folder root = staticFunctionsTest.initConfig(config);
        Profile profile = config.getProfile();
                
        //create v1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");
        CloneFile dbFile = staticFunctionsTest.indexNewRequest(root, f1, null);        
        Long fileId = dbFile.getId();
                
        dbFile.setSyncStatus(SyncStatus.UPTODATE);
        dbFile.setServerUploadedAck(true);
        dbFile.setServerUploadedTime(new Date());
        
        dbFile.merge();
                
        //delete v2
        DeleteIndexRequest deleteIndex = new DeleteIndexRequest(root, dbFile);        
        deleteIndex.process();        
        
        Thread.sleep(1000); 
        dbFile = db.getFileOrFolder(fileId, 2);
        
        if(f1.exists()){
            f1.delete();
        }

        //create a conflicted modification v2
        Update update = Update.parse(dbFile);
        update.setVersion(2);
        update.setStatus(Status.CHANGED);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());  
        
        List<Update> list = new ArrayList<Update>();
        list.add(update);
                
        ChangeManager cm = profile.getRemoteWatcher().getChangeManager();
        cm.queueUpdates(list);
        System.out.println("FINISH!!!!\n\n");        
        System.out.println("The result is: \n file1 with content1. \n 3 rows in database File1 New ACK, Deleted ACK | File1 conflicted New.");
        while(true) { }
    }    
}
