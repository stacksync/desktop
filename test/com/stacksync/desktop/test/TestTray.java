/*
 * Syncany
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

import java.io.FileNotFoundException;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.tray.TrayEvent;
import com.stacksync.desktop.gui.tray.TrayEventListener;

/**
 *
 * @author Philipp C. Heckel
 */
public class TestTray {
    public static void main(String[] a) throws InterruptedException, FileNotFoundException, InitializationException, ConfigException {
    Config.getInstance().load();
	
    Tray tray = Tray.getInstance();
    tray.registerProcess(tray.getClass().getSimpleName());
    tray.init("Everything is up to date.");
    tray.addTrayEventListener(new TrayEventListener() {

        @Override
        public void trayEventOccurred(TrayEvent event) {
        System.out.println(event.getType()+" "+event.getArgs());
        }
    });

    while (true) {
        tray.notify("Test notification", "blabblkablbfldsfdslfkj hsfkjhsf sfkjshf skdjfhs df", null);
        System.out.println("test");
        tray.setStatusIcon(tray.getClass().getSimpleName(), Tray.StatusIcon.UPDATING);
        Thread.sleep(5000);

        tray.setStatusIcon(tray.getClass().getSimpleName(), Tray.StatusIcon.UPTODATE);
        Thread.sleep(5000);
    }

    }
}
