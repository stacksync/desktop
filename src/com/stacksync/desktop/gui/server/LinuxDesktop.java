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

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.CloneFile;
import java.io.File;
import org.apache.log4j.Logger;

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
public class LinuxDesktop extends Desktop {
    private final Logger logger = Logger.getLogger(LinuxDesktop.class.getName());
    private static final Config config = Config.getInstance();
      
    private TouchServer touchServ;
    private CommandServer commandServ;
 
    protected LinuxDesktop() {
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
    public void touch(File file, CloneFile.SyncStatus status) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void stop(boolean startDemonOnly) {
        
        if (!startDemonOnly) {
            touchServ.setRunning(false);
        }
        commandServ.setRunning(false);
    }

}
