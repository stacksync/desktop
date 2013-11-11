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
package com.stacksync.desktop.index.requests;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.gui.server.Desktop;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.chunker.Chunker;

/**
 *
 * @author Philipp C. Heckel
 */
public abstract class IndexRequest {
    
    protected Config config = Config.getInstance();
    protected DatabaseHelper db = DatabaseHelper.getInstance();
    protected final Tray tray = Tray.getInstance();
    protected final String processName;
    
    //protected final Chunker chunker = Chunker.factoryCreateChunker();
    protected final Chunker chunker = new Chunker();
    protected final Desktop desktop = Desktop.getInstance();
    
    public IndexRequest() { 
        processName = this.getClass().getSimpleName();
        tray.registerProcess(processName);        
    }
    
    public abstract void process();
}
