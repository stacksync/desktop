package com.stacksync.desktop.db.models;

import java.io.File;
import java.io.Serializable;
import javax.persistence.*;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;


public class CloneClientPk implements Serializable {
    private static final Config config = Config.getInstance();
    private static final long serialVersionUID = 17398420202020111L;

    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="id")
    private Long id;

    @Id
    @Column(name="profile_id", nullable=false)
    private Long profileId;

    @Id
    @Column(name="machinename")
    private String machineName;        

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String name) {
        this.machineName = name;
    }
    
    public File getUserImageFile() {
        return new File(config.getConfDir()+File.separator+Constants.PROFILE_IMAGE_DIRNAME+File.separator+getProfileId()+"-"+getMachineName()+".png");	
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneClientPk)) {
            return false;
        }

        CloneClientPk other = (CloneClientPk) object;

        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CloneClient[id=" + id + ", profile_id=" + profileId + ", name=" + machineName + "]";
    }

}
