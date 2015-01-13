/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.cache;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.periodic.CacheCleaner;
import com.stacksync.desktop.test.staticFunctionsTest;

/**
 *
 * @author gguerrero
 */
public class TestCache {

    private static Config config = Config.getInstance(); 

    public static void main(String[] args) throws Exception {        
        staticFunctionsTest.initConfig(config);
                
        CacheCleaner cache = new CacheCleaner();
        cache.start();
        
        while(true) { }
    }
}
