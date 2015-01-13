package com.stacksync.desktop.test.watcher;

import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.watch.local.LocalWatcher;
import java.io.File;

public class WatcherTest {
    
    LocalWatcher watcher;
    Profile profile;
    Folder folder;
    
    public WatcherTest() {
        folder = new Folder();
        folder.setActive(true);
        File folderFile = new File("C:\\Users\\lab144\\watcher_test");
        folder.setLocalFile(folderFile);
        
        profile = new Profile();
        profile.setFolder(folder);
        
        watcher = LocalWatcher.getInstance();
        watcher.watch(profile);
        watcher.start();
    }
    
    public static void main(String[] args){
        WatcherTest test = new WatcherTest();
    }
    
}
