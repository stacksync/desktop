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
package com.stacksync.desktop.db.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import com.stacksync.desktop.db.PersistentObject;

/**
 * Represents the chunk of a file.
 * 
 * @author Philipp C. Heckel
 */
@Entity
public class CloneChunk extends PersistentObject implements Serializable {
    private static final long serialVersionUID = 3232299912L;
    
    public enum CacheStatus { REMOTE, CACHED };
    
    @Id
    @Column(name="checksum")
    private String checksum;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CacheStatus status;

    public CloneChunk() { 
        this.status = CacheStatus.CACHED;
    }

    public CloneChunk(String checksum, CacheStatus status) {
        this.checksum = checksum;
        this.status = status;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getFileName() {
        return String.format("chk-%s", getChecksum());
    }
    
    public void setCacheStatus(CacheStatus status) {
        this.status = status;
    }
    
    public CacheStatus getCacheStatus(){
        return status;
    }

    @Override
    public int hashCode() {
        return checksum.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneChunk)) {
            return false;
        }
        
        CloneChunk other = (CloneChunk) object;
        return this.checksum.compareTo(other.checksum) == 0;        
    }

    @Override
    public String toString() {
        return "CloneChunk[ checksum=" + checksum + "]";
    }
}
