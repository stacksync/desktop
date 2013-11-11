/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test;

import java.io.File;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.index.Indexer;

/**
 *
 * @author gguerrero
 */
public class TestIndexer {

    private static Config config = Config.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance();          
    
    public static void main(String[] args) throws Exception {        
        staticFunctionsTest.initConfig(config);
        //EntityManager em = config.getDatabase().getEntityManager();
        Indexer indexer = Indexer.getInstance();
        indexer.start();
        
        Profile profile = config.getProfiles().get(1);
                
        Folder root = profile.getFolders().list().get(0);
        staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content4");
        staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName2, "content5");
        staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName3, "content6");        
        indexer.index(profile);
        
        Thread.sleep(1000);
        
        File f2 = new File(root.getLocalFile().getPath() + staticFunctionsTest.fileName2);
        File f3 = new File(root.getLocalFile().getPath() + staticFunctionsTest.fileName3);
        
        CloneFile dbFile = db.getFile(root, f3);
        
        dbFile.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        dbFile.setSyncStatus(CloneFile.SyncStatus.REMOTE);
        
        dbFile.merge();
        
        f2.delete();
        f3.delete();
        
        Thread.sleep(1000);
        indexer.index(profile);
        
        System.out.println("FINISH!!!!\n\n");        
        System.out.println("The result is: \n file1 with content4. \n 4 rows in database File1 New | File2 New, Deleted | File3 New(Remote).");
        
        while(true) { }
    }
}
