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
package com.stacksync.desktop.periodic;

import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.index.Indexer;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 * Prevents missed updates by regularly searching the whole file tree for new or
 * altered files.
 *
 * TODO Implement this
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TreeSearch {

    private final Logger logger = Logger.getLogger(TreeSearch.class.getName());
    private final Config config = Config.getInstance();    
    private Timer timer;

    public TreeSearch() {
        timer = null;
    }

    public synchronized void start() {        
        timer = new Timer("TreeSearch");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doPeriodicCheck();
            }
        }, 0, Constants.PERIODIC_SEARCH_INTERVAL);
        
    }

    public synchronized void stop() {       
        if (timer == null) {
            return;
        }
        
        timer.cancel();
        timer = null;
    }
    
    
    private void doPeriodicCheck(){
        logger.debug("Started periodic tree search ...");

        try {
            Profile profile = config.getProfile();
            if (!profile.isEnabled()) {
                return;
            }

            while(profile.getRemoteWatcher().getChangeManager().queuesUpdatesIsWorking()){ 
                Thread.sleep(1000);
            }                        

            logger.debug("Checking profile "+profile.getName()+" ...");                        
            Indexer.getInstance().index(profile);
                
            logger.debug("Finished periodic tree search. Now sleeping "+Constants.PERIODIC_SEARCH_INTERVAL+" seconds.");
        } catch (InterruptedException ex) {
            logger.error("PeriodicTreeSearch catches exception", ex);
        }
    }
}
