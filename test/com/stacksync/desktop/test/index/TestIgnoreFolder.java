/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.index;

import java.io.File;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestIgnoreFolder {
    private static Config config = Config.getInstance();
    
    public static void main(String[] args) throws Exception {
        Folder root = staticFunctionsTest.initConfig(config);
                
        //create F1
        File folder = new File(root.getLocalFile().getPath() + File.separator + ".ignore-folder1");
        if(!folder.exists()){
            folder.mkdirs();
        }
        
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + File.separator + ".ignore-folder1" + 
                                                 File.separator + staticFunctionsTest.ignoreFileName1, "content1");
        
        File f2 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + File.separator + ".ignore-folder1" + 
                                                 File.separator + staticFunctionsTest.fileName2, "content2");
        
        
        folder = new File(root.getLocalFile().getPath() + File.separator + "folder1");
        if(!folder.exists()){
            folder.mkdirs();
        }
        
        File f3 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + File.separator + "folder1" + 
                                                 File.separator + staticFunctionsTest.ignoreFileName1, "content2");        
                
        
        staticFunctionsTest.indexNewRequest(root, folder, null);
        staticFunctionsTest.indexNewRequest(root, f1, null);
        staticFunctionsTest.indexNewRequest(root, f2, null);
        staticFunctionsTest.indexNewRequest(root, f3, null);
        
        System.out.println("The result is: \n ignore folder1 exist with 2 files. \n 0 rows in database New.");
        while(true) { }
    }  
}
