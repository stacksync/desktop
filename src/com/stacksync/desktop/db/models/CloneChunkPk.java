package com.stacksync.desktop.db.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Id;


public class CloneChunkPk implements Serializable {
    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name="checksum")
    private String checksum;
    
    @Id
    @Column(name="chunkpath", nullable=false)
    private String chunkpath = "";


    @Id
    @Column(name="chunk_ORDER", nullable=false)
    private int chunkOrder;     
    
    public String getChecksum() {
        return checksum;
    }

    public void setPath(String path){
        this.chunkpath = path;
    }

    public String getFileName() {
        if(chunkpath.compareTo("/") == 0){
            return String.format("%schk-%s", chunkpath, getChecksum());
        } else{
            return String.format("%s/chk-%s", chunkpath, getChecksum());
        }
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
        return "CloneChunk[path=" + chunkpath + ", checksum=" + checksum + "]";
    }
}
