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
package com.stacksync.desktop.gui.tray.platform;

import com.stacksync.desktop.gui.linux.LinuxNativeClient;
import com.stacksync.desktop.gui.linux.UpdateStatusIconRequest;
import com.stacksync.desktop.gui.linux.NotifyRequest;
import com.stacksync.desktop.gui.linux.UpdateStatusTextRequest;
import com.stacksync.desktop.gui.linux.ListenForTrayEventRequest;
import com.stacksync.desktop.gui.linux.UpdateMenuRequest;
import java.io.File;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.tray.TrayEvent;
import com.stacksync.desktop.gui.tray.TrayEventListener;


/**
 *
 * @author Philipp C. Heckel
 */
public class LinuxTray extends Tray {
    private LinuxNativeClient nativeClient;
    private boolean initialized = false;    
    
    public LinuxTray() {
        super();	     
    }

    @Override
    public void init(String initialMessage) throws InitializationException {
        nativeClient = LinuxNativeClient.getInstance();
        nativeClient.init(initialMessage);
        addListener();

        initialized = true;	
        updateUI();	
    }

    @Override
    public synchronized void destroy() {
        if (!initialized) {
            logger.warn(config.getMachineName()+"#Cannot destroy. Tray not initialized yet.");                
            return;
        }
        
        nativeClient.destroy();        
    }
    
    @Override
    public void setStatusIconPlatform(StatusIcon status) {
        if (!initialized) {
            logger.debug(config.getMachineName()+"#Cannot change icon. Tray not initialized yet.");                
            return;
        }
                
        nativeClient.send(new UpdateStatusIconRequest(status));
    }    

    @Override
    public void notify(String summary, String body, File imageFile) {
        if (!initialized) {
            logger.warn(config.getMachineName()+"#Cannot send notification. Tray not initialized yet.");                
            return;
        }
	
        nativeClient.send(new NotifyRequest(summary, body, imageFile));
    }
    
    @Override
    public void updateUI() {
        if (!initialized) {
            logger.warn(config.getMachineName()+"#Cannot update tray menu. Tray not initialized yet.");                
            return;
        }

        UpdateMenuRequest menu = new UpdateMenuRequest(config.getProfiles().list());       
        nativeClient.send(menu);
    }
    
    @Override
    public void updateStatusText() {
        if (!initialized) {
            logger.warn(config.getUserName()+"#Cannot update status. Tray not initialized yet.");                
            return;
        }
        
        UpdateStatusTextRequest menu = new UpdateStatusTextRequest(processesText);
        nativeClient.send(menu);
    }
    
    
    public static void main(String[] args) throws ConfigException, InitializationException, InterruptedException {
        System.out.println("STARTED");

        //for (Entry<Object, Object> entry : System.getProperties().entrySet()) 
          //  System.out.println(entry.getKey() + " = "+entry.getValue());

        config.load();
        Tray tray = Tray.getInstance();
        tray.registerProcess(tray.getInstance().getClass().getSimpleName());
        tray.init("Everything is up to date.");

        tray.notify("hello", "test", null);
        tray.setStatusIcon(tray.getInstance().getClass().getSimpleName(), StatusIcon.UPDATING);
        tray.setStatusText(tray.getInstance().getClass().getSimpleName(), "hello!");
        
        
        Thread.sleep(1000);
        tray.setStatusIcon(tray.getInstance().getClass().getSimpleName(), StatusIcon.UPTODATE);
        
        tray.addTrayEventListener(new TrayEventListener() {

            @Override
            public void trayEventOccurred(TrayEvent event) {
            System.out.println(event);
            }
        });
        
        //System.out.println(FileUtil.showBrowseDirectoryDialog());

        while(true) {
            Thread.sleep(1000);
        }
	
    }

    private void addListener() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Object event = nativeClient.send(new ListenForTrayEventRequest());

                    if (event != null) {
                        fireTrayEvent((TrayEvent) event);
                    }
                }
            }
        }, "TrayListener").start();
    }

}
