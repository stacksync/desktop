package com.stacksync.desktop.db.models;

import java.io.Serializable;
import javax.persistence.*;


public class CloneFilePk implements Serializable {
    
    private static final long serialVersionUID = 12314234L;

    @Id
    @Column(name = "file_id", nullable = false)
    private Long id;
    @Id
    @Column(name = "file_version", nullable = false)
    private long version;
    
    
    public CloneFilePk() { }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Long getFileId() {
        return id;
    }

    public void setFileId(Long fileId) {
        this.id = fileId;
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        hash += version;
        return hash;
    }   

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CloneFilePk other = (CloneFilePk) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CloneFile[id=" + id + ", version=" + version + "]";
    }
}