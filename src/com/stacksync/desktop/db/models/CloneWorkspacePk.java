package com.stacksync.desktop.db.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Id;


public class CloneWorkspacePk implements Serializable {
    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name="id", nullable=false)
    private Long id;
        
    public Long getId() {
        return id;
    } 
    
    public void setId(Long id) {
        this.id = id;
    }     

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneWorkspacePk)) {
            return false;
        }
        
        CloneWorkspacePk other = (CloneWorkspacePk) object;
        return this.id.compareTo(other.id) == 0;        
    }

    @Override
    public String toString() {
        return "Workspace[id=" + id + "]";
    }            
}
