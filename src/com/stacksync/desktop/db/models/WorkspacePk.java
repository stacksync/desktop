package com.stacksync.desktop.db.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Id;


public class WorkspacePk implements Serializable {
    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name="id", nullable=false)
    private String id;
    
    @Id
    @Column(name="profile_id_work", nullable=false)
    private int profileId;
        

    public String getId() {
        return id;
    } 
    
    public void setId(String id) {
        this.id = id;
    }     
    
    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof WorkspacePk)) {
            return false;
        }
        
        WorkspacePk other = (WorkspacePk) object;
        return this.id.compareTo(other.id) == 0;        
    }

    @Override
    public String toString() {
        return "Workspace[id=" + id + ", profile_id=" + profileId + "]";
    }            
}
