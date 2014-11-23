package com.stacksync.desktop.gui.server;

import com.stacksync.desktop.Environment;
import com.stacksync.desktop.db.models.CloneFile;
import java.io.File;
import org.apache.log4j.Logger;

public abstract class Desktop {
    private final Logger logger = Logger.getLogger(Desktop.class.getName());
    private static final Environment env = Environment.getInstance();
    
    private static Desktop instance;    
 
    protected Desktop() {
        logger.info("Creating desktop integration ...");
    }
    
    public synchronized static Desktop getInstance() {
        if (instance == null) {
            
            switch (env.getOperatingSystem()) {
                case Linux:
                    instance = new LinuxDesktop();
                    break;
                case Windows:
                    instance = new WindowsDesktop();
                    break;
                case Mac:
                    instance = new MacDesktop();
                    break;
            }
            
        }
        
        return instance;
    }

    public abstract void start(boolean startDemonOnly);

    public abstract void touch(File file);
    
    public abstract void touch(String filepath, CloneFile.SyncStatus status);
    
    public abstract void untouch(String filepath);
    
    public abstract void stop(boolean startDemonOnly);
    
}
