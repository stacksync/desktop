/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.cache;

import java.io.File;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.periodic.CacheCleaner;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestCacheNewFile {

    private static Config config = Config.getInstance(); 

    public static void main(String[] args) throws Exception {        
        Folder root = staticFunctionsTest.initConfig(config);
                
        CacheCleaner cache = new CacheCleaner();        
        
        //create F1
        File f1 = staticFunctionsTest.createFile(root.getLocalFile().getPath() + staticFunctionsTest.fileName1, "content1");
        staticFunctionsTest.indexNewRequest(root, f1, null);
        
        cache.start();
        
        while(true) { }
    }
}
