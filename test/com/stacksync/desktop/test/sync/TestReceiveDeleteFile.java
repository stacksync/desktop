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
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.test.staticFunctionsTest;
import com.stacksync.desktop.watch.remote.ChangeManager;

/**
 *
 * @author gguerrero
 */
public class TestReceiveDeleteFile {

    private static Config config = Config.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance();  

    public static void main(String[] args) throws Exception {       
        Folder root = staticFunctionsTest.initConfig(config);
        
        
        //create v1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");
        CloneFile dbFile = staticFunctionsTest.indexNewRequest(root, f1, null);
        
        dbFile.setSyncStatus(SyncStatus.UPTODATE);
        dbFile.setServerUploadedAck(true);
        dbFile.setServerUploadedTime(new Date());
        
        dbFile.merge();
        
        
        //create v2
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content2");
        dbFile = staticFunctionsTest.indexNewRequest(root, f1, dbFile);
        
        dbFile.setSyncStatus(SyncStatus.UPTODATE);
        dbFile.setServerUploadedAck(true);
        dbFile.setServerUploadedTime(new Date());
        
        dbFile.merge();
                
        //create a delete V3
        Update update = Update.parse(dbFile);        
        update.setVersion(3);
        update.setStatus(Status.DELETED);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());  
        
        List<Update> list = new ArrayList<Update>();
        list.add(update);
        
        Profile profile = config.getProfile();
        ChangeManager cm = profile.getRemoteWatcher().getChangeManager();
        cm.queueUpdates(list);
        
        System.out.println("FINISH!!!!\n\n");
        System.out.println("The result is: \n no file. \n 3 rows in database New ACK, Changed ACK, Deleted ACK.");
        while(true) { }
    }
}
