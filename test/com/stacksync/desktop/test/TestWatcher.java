/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.test;

import java.io.File;
import java.io.IOException;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.contrib.BufferedWatcher;
import name.pachler.nio.file.contrib.WatchListener;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TestWatcher {    
    private static final BufferedWatcher w = new BufferedWatcher();
    private static final int timer = 5000;
    
    private static void watchFolder(String folder) throws IOException{
        File file = new File(folder);
        
        if(file.exists()){
            WatchKey wFolder = w.addWatch(file, true, new WatchListener() {
                @Override
                public void watchEventOccurred(WatchKey rootKey, WatchEvent event) {
                    System.out.println("Folder root =" +rootKey+", event = "+event);
                }
            });
            
            System.out.println("Loaded folder -> " + folder);
        } else{
            System.out.println("Error -> Folder doesn't exist! " + folder);
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {                
        watchFolder("/home/gguerrero/test");
        watchFolder("c:\\test");
        w.start();
        
        while (true) {            
            Thread.sleep(timer);
            System.out.println("Thread sleep(" + timer + ")!");
            //w.printMaps();
        }
    }
}
