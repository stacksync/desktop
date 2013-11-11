/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.index;

import java.io.File;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.index.requests.DeleteIndexRequest;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestDeleteVersion {

    private static Config config = Config.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance(); 

    public static void main(String[] args) throws Exception {        
        Folder root = staticFunctionsTest.initConfig(config);
                
        //create F1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");                
        CloneFile dbFile = staticFunctionsTest.indexNewRequest(root, f1, null);
        
        //create F2
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content2");                
        staticFunctionsTest.indexNewRequest(root, f1, dbFile);
        
        DeleteIndexRequest delete = new DeleteIndexRequest(root, f1);
        delete.process();
        
        Thread.sleep(1000); 
        dbFile = db.getFile(root, f1);
        System.out.println(dbFile);
        
        if(f1.exists()){
            f1.delete();
        }
        
        System.out.println("The result is: \n file1 doesn't exist. \n 3 rows in database New, Changed, Deleted.");
        while(true) { }
    }
}
