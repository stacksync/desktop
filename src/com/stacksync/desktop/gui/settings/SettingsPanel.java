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
package com.stacksync.desktop.gui.settings;

import java.util.ResourceBundle;
import javax.swing.JPanel;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class SettingsPanel extends JPanel {
    protected final Config config = Config.getInstance();
    protected final ResourceBundle resourceBundle = Config.getInstance().getResourceBundle();
    protected Profile profile;
    
    public abstract void clean();
    public abstract void load();
    public abstract void save();
    public abstract boolean check();
}
