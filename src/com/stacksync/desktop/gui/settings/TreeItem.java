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

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class TreeItem {
    private String description;
    private String iconFilename;

    public TreeItem(String description, String iconFilename) {
        this.description = description;
        this.iconFilename = iconFilename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }        

    public String getIconFilename() {
        return iconFilename;
    }

    @Override
    public String toString() {
        return description;
    }

}
