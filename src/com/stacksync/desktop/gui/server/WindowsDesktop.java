package com.stacksync.desktop.gui.server;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.exceptions.ConfigException;
import java.io.File;
import org.apache.log4j.Logger;

public class WindowsDesktop extends Desktop {
    private final Logger logger = Logger.getLogger(WindowsDesktop.class.getName());
    private static final Config config = Config.getInstance();
    
    private TouchServer touchServ;
    private CommandServer commandServ;
 
    protected WindowsDesktop() {
        logger.info("Creating desktop integration ...");
        touchServ = new TouchServer();
        commandServ = new CommandServer();
    }

    @Override
    public void start(boolean startDemonOnly) {
        logger.info("Starting desktop services (daemon: " + startDemonOnly + ") ...");
        
        if(!startDemonOnly){
            new Thread(touchServ, "Touch Server").start();
        }
        
        new Thread(commandServ, "Command Server").start();
    }

    @Override
    public void touch(File file) {
        if (!touchServ.isRunning()) {
            logger.debug("Warning: Touch server NOT RUNNING. Ignoring touch to "+file);           
            return;
        }

        touchServ.touch(file);
    }
    
    @Override
    public void stop(boolean startDemonOnly) {
        
        if (!startDemonOnly) {
            touchServ.setRunning(false);
        }
        commandServ.setRunning(false);
    }

    public static void main(String[] a) throws InterruptedException, ConfigException {
        config.load();
        
        WindowsDesktop desk = new WindowsDesktop();
        desk.start(false);

        while (true) {
            Thread.sleep(1000);
        }
    }

}
