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
package com.stacksync.desktop.gui.server;

import java.io.File;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 * The Stacksync file manager extension is strongly based on the Dropbox file
 * manager extension. It uses two servers to communicate with the extension:
 * 
 * <p>The {@link CommandServer} answers queries by file manager, e.g. regarding
 * emblems and popup-menu entries.
 * 
 * <p>The {@link TouchServer} can send 'touch' queries to the file manager
 * extension. A 'touch' invalidates the emblem-status of a file and forces
 * the file manager to re-query the information using the command server.
 * 
 * @author Philipp C. Heckel
 * @see <a href="https://www.dropbox.com/downloading?os=lnx">Dropbox Nautilus
 *      extension</a> (dropbox.com)
 */
public class Desktop {
    private final Logger logger = Logger.getLogger(Desktop.class.getName());
    private static final Config config = Config.getInstance();
    
    private static Desktop instance;    
    private TouchServer touchServ;
    private CommandServer commandServ;
 
    private Desktop() {
        logger.info(config.getMachineName()+"#Creating desktop integration ...");
        touchServ = new TouchServer();
        commandServ = new CommandServer();
    }
    
    public synchronized static Desktop getInstance() {
        if (instance == null) {
            instance = new Desktop();
        }
        
        return instance;
    }

    public void start(boolean startDemonOnly) {
        logger.info(config.getMachineName()+"#Starting desktop services (daemon: " + startDemonOnly + ") ...");
        
        if(!startDemonOnly){
            new Thread(touchServ, "Touch Server").start();
        }
        
        new Thread(commandServ, "Command Server").start();
    }

    public void touch(File file) {
        if (!touchServ.isRunning()) {
            logger.debug(config.getMachineName()+"#Warning: Touch server NOT RUNNING. Ignoring touch to "+file);           
            return;
        }

        touchServ.touch(file);
    }

    public static void main(String[] a) throws InterruptedException, ConfigException {
        config.load();
        
        Desktop desk = new Desktop();
        desk.start(false);

        while (true) {
            Thread.sleep(1000);
        }
    }

}
