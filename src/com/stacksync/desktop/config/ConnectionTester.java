/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.config;

import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.exceptions.StorageConnectException;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionTester {
    
    private final int INTERVAL;
    
    private ConnectionController controller;
    private Config config;
    private Timer timer;
    
    public ConnectionTester(ConnectionController controller) {
        this.INTERVAL = 20000;
        this.controller = controller;
        this.config = Config.getInstance();
    }
    
    public void start() {
        
        
        timer = new Timer("RemoteWatcher");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                test();
            }
        }, 0, INTERVAL);
    }
    
    public synchronized void stop() {
        if (timer == null) {
            return;
        }                
        
        timer.cancel();
        timer = null;
    } 
    
    private void test() {
        
        boolean connection = true;
        
        Profile profile = config.getProfile();
        
        if (profile == null) {
            return;
        }

        
        TransferManager tm = profile.getRepository().getConnection().createTransferManager();
        try {
            tm.connect();
        }  catch (StorageConnectException ex) {

            connection = false;
            // This error is from connect() function
            // It is necessary to differenciate between unauthorize, no connection or others...
            String message = ex.getCause().getMessage();
            if (message.contains("Incorrect")) {
                // Unauthorize
            } else if (message.contains("not known") || message.contains("unreachable")) {
                // Network problems
            } else {
                // Unknown problems...
            }

        }
        
        if (connection) {
            this.controller.connectionEstablished();
        }
    }
    
}
