package com.stacksync.desktop.db.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Id;


public class CloneChunkPk implements Serializable {
    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name="checksum")
    private String checksum;
    
    public String getChecksum() {
        return checksum;
    }

    @Override
    public int hashCode() {
        return checksum.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneChunkPk)) {
            return false;
        }
        
        CloneChunkPk other = (CloneChunkPk) object;
        return this.checksum.compareTo(other.checksum) == 0;        
    }

    @Override
    public String toString() {
        return "CloneChunk[ checksum=" + checksum + "]";
    }
}
