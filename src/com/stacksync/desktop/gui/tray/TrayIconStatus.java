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
package com.stacksync.desktop.gui.tray;

import java.util.Timer;
import java.util.TimerTask;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.gui.tray.Tray.StatusIcon;

/**
 *
 * Note: this class is used by java-gnome as well as Swing. Make sure that it
 * does not contain any components of the two frameworks.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TrayIconStatus {
    protected final long UPDATE_ICON_INTERVAL = 400;
    protected final long UPDATE_ICON_COUNT = 5;
    
    private StatusIcon status;
    private TrayIconStatusListener listener;
    
    private Timer iconUpdatingTimer;
    private int iconUpdatingCurrentImg;    
    private int updatingCount;
    
    public TrayIconStatus(TrayIconStatusListener listener) {
        this.status = StatusIcon.DISCONNECTED;
        this.listener = listener;        
        this.updatingCount = 0;
    }
    
    public synchronized StatusIcon setIcon(StatusIcon status) {
        if (status == StatusIcon.DISCONNECTED) {
            updatingCount = 0;
            listener.trayIconUpdated(Constants.TRAY_FILENAME_DEFAULT);
            
            this.status = StatusIcon.DISCONNECTED;
            return this.status;
        }
        
        else if (status == StatusIcon.UPDATING) {
            updatingCount++;
            
            if (updatingCount == 1) {
                iconUpdatingTimer = new Timer("TrayIconUpdating");            
                iconUpdatingTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override public void run() { doUpdateStatusIcon(); } }, 0, UPDATE_ICON_INTERVAL);
            }
            
            this.status = StatusIcon.UPDATING;
            return this.status;
        }
        
        else if (status == StatusIcon.UPTODATE) {
            updatingCount = (updatingCount <= 1) ? 0 : updatingCount-1;
            
            if (updatingCount == 0) {
                if (iconUpdatingTimer != null) {
                    iconUpdatingTimer.cancel();
                    iconUpdatingTimer = null;
                }                

                listener.trayIconUpdated(Constants.TRAY_FILENAME_UPTODATE);
                
                this.status = StatusIcon.UPTODATE;
                return this.status;
            }
            else {
                this.status = StatusIcon.UPDATING;
                return this.status;
            }
        }
        
        // Cannot happen
        this.status = StatusIcon.UPDATING;
        return this.status;
    }    
    
    public StatusIcon getIcon() {
        return status;
    }
 
    private void doUpdateStatusIcon() {
        listener.trayIconUpdated(String.format(Constants.TRAY_FILENAME_FORMAT_UPDATING, iconUpdatingCurrentImg));
        
        iconUpdatingCurrentImg++;

        if (iconUpdatingCurrentImg > UPDATE_ICON_COUNT) {
            iconUpdatingCurrentImg = 1;
        }
    }    
    
    public static interface TrayIconStatusListener {
        public void trayIconUpdated(String filename);
    }
}
