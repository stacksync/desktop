/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.index;

import java.io.File;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestIgnoreFile {
    private static Config config = Config.getInstance();
    
    public static void main(String[] args) throws Exception {               
        Folder root = staticFunctionsTest.initConfig(config);
                
        //create F1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.ignoreFileName1, "content1");        
        staticFunctionsTest.indexNewRequest(root, f1, null);
        
        System.out.println("The result is: \n ignore file1 exist with content1. \n 0 rows in database New.");
        while(true) { }
    }  
}
