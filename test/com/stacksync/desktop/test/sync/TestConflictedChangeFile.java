/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.test.staticFunctionsTest;
import com.stacksync.desktop.watch.remote.ChangeManager;

/**
 *
 * @author gguerrero
 */
public class TestConflictedChangeFile {

    private static Config config = Config.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance();
       
    public static void doProcess(String fileName) throws Exception{
        Folder root = staticFunctionsTest.initConfig(config);
        EntityManager em = config.getDatabase().getEntityManager();
        
        //create v1 ACK
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + fileName, "content1");
        CloneFile dbFile = staticFunctionsTest.indexNewRequest(root, f1, null);
        
        dbFile.setSyncStatus(SyncStatus.UPTODATE);
        dbFile.setServerUploadedAck(true);
        dbFile.setServerUploadedTime(new Date());
        
        dbFile.merge();
                
        //create v2
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + fileName, "content2");
        dbFile = staticFunctionsTest.indexNewRequest(root, f1, dbFile);       
                
        //create v3
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + fileName, "content3");
        dbFile = staticFunctionsTest.indexNewRequest(root, f1, dbFile); 
        
        //create v4
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + fileName, "content4");
        dbFile = staticFunctionsTest.indexNewRequest(root, f1, dbFile);
        
        //create v5
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + fileName, "content5");        
        staticFunctionsTest.indexNewRequest(root, f1, dbFile);
        
        //remove v5        
        em.getTransaction().begin();
        dbFile = db.getFile(root, f1);
        
        Object toBeRemoved = em.merge(dbFile);
        em.remove(toBeRemoved);
        
        em.flush();
        em.getTransaction().commit();
        
        staticFunctionsTest.createFile(root.getLocalFile().getPath() + fileName, "content4");
        //create a conflicted modification v2
        Update update = Update.parse(dbFile);        
        update.setVersion(2);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());  
        
        List<Update> list = new ArrayList<Update>();
        list.add(update);
        
        Profile profile = config.getProfiles().get(1);
        ChangeManager cm = profile.getRemoteWatcher().getChangeManager();        
        cm.queueUpdates(list);
        
        System.out.println("FINISH!!!!\n\n");        
        System.out.println("The result is: \n file1 with content5. \n file1 conflicted with content4. \n 5 rows in database File1 New ACK, Changed ACK | File1 conflicted New, Changed, Changed.");
        while(true) { }
    }
    
    
    public static void main(String[] args) throws Exception {        
        doProcess(staticFunctionsTest.fileName1);
    }
}
