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
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.test.staticFunctionsTest;
import com.stacksync.desktop.watch.remote.ChangeManager;

/**
 *
 * @author gguerrero
 */
public class TestConflictedNewFile {

    private static Config config = Config.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance();
    
    public static void main(String[] args) throws Exception {
        EntityManager em = config.getDatabase().getEntityManager();
        Folder root = staticFunctionsTest.initConfig(config);
        
        
        //create F1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");
        CloneFile dbFile = staticFunctionsTest.indexNewRequest(root, f1, null);
        
        //create F2
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content2");        
        staticFunctionsTest.indexNewRequest(root, f1, dbFile);
                      
        Thread.sleep(5000);
        
        em.getTransaction().begin();
        dbFile = db.getFile(root, f1);                
        
        Object toBeRemoved = em.merge(dbFile);
        em.remove(toBeRemoved);
        
        em.flush();
        em.getTransaction().commit();
                        
        
        staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");
        //create a conflicted modification v2
        Update update = Update.parse(dbFile);
        update.setVersion(1);
        update.setStatus(Status.NEW);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());  
        
        List<Update> list = new ArrayList<Update>();
        list.add(update);
        
        Profile profile = config.getProfile();
        ChangeManager cm = profile.getRemoteWatcher().getChangeManager();
        cm.queueUpdates(list);                
        System.out.println("FINISH!!!!\n\n");        
        System.out.println("The result is: \n file1 exist with content2. \n file1 conflicted with content1. \n 2 rows in database New ACK, New conflicted.");
        while(true) { }
    }
}
