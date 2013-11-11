/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.index;

import java.io.File;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestAddChangeVersion {
    private static Config config = Config.getInstance();

    public static void main(String[] args) throws Exception {                         
        Folder root = staticFunctionsTest.initConfig(config);
                
        //create F1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + File.separator + "file1", "content1");                
        CloneFile dbFile = staticFunctionsTest.indexNewRequest(root, f1, null);
        
        //create F2
        f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + File.separator + "file1", "content2");        
        staticFunctionsTest.indexNewRequest(root, f1, dbFile);
        
        System.out.println("The result is: \n file1 with content2. \n 2 rows in database New, Changed.");
        while(true) { }
    }
}
