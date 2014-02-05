package com.stacksync.desktop.db.models;

import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.wizard.RepositoryTestPanel.TestListener;
import com.stacksync.desktop.syncserver.Server;
import com.stacksync.commons.models.Workspace;

@Entity
@IdClass(value = CloneWorkspacePk.class)
public class CloneWorkspace extends PersistentObject implements Serializable {
        
    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name="id", nullable=false)
    private Long id;
    
    @Column(name="path_workspace", nullable=false)
    private String pathWorkspace;
    
    @Column(name="local_revision", nullable=false)
    private Integer localLastUpdate;
        
    @Column(name="remote_revision", nullable=false)
    private Integer remoteLastUpdate;
            
    @OneToMany
    private List<CloneFile> files;
    
    public CloneWorkspace(){}
    
    public CloneWorkspace(Workspace r){
        this.id = r.getId();
        //this.pathWorkspace = r.getPath();
        this.pathWorkspace = "/";
        this.localLastUpdate = r.getLatestRevision();
        this.remoteLastUpdate = r.getLatestRevision();
    }

    public Long getId() {
        return id;
    }
    
    public String getPathWorkspace(){
        return pathWorkspace;
    }

    public Integer getLocalLastUpdate() {
        return localLastUpdate;
    }
    
    public void setLocalLastUpdate(Integer localLastUpdate) {
        this.localLastUpdate = localLastUpdate;
    }
    
    public Integer getRemoteRevision() {
        return remoteLastUpdate;
    }
    
    public void setRemoteRevision(Integer remoteLastUpdate) {
        this.remoteLastUpdate = remoteLastUpdate;
    }    

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneWorkspace)) {
            return false;
        }
        
        CloneWorkspace other = (CloneWorkspace) object;
        return this.id.compareTo(other.id) == 0;        
    }

    @Override
    public String toString() {
        return "CloneWorkspace[id=" + id + ", pathWorkspace=" + pathWorkspace + ", localLastUpdate=" + localLastUpdate + ", remoteLastUpdate=" + remoteLastUpdate + "]";
    }
        
    public List<CloneFile> getFiles(){
        return this.files;
    }
    
    public static Map<Long, CloneWorkspace> InitializeWorkspaces(Profile profile, final TestListener callbackListener)
            throws InitializationException{
                  
        List<CloneWorkspace> remoteWorkspaces = new ArrayList<CloneWorkspace>();
                                            
        try {
            Server server = profile.getServer();
            remoteWorkspaces = server.getWorkspaces(profile.getCloudId());
        } catch (NoWorkspacesFoundException ex) {
            if(callbackListener != null){
                callbackListener.setStatus("Can't load the workspaces from syncserver. ");
                callbackListener.setError(ex);
                callbackListener.actionCompleted(false);
            }

            throw new InitializationException("Can't load the workspaces from syncserver. ", ex);
        }

        DatabaseHelper db = DatabaseHelper.getInstance();
        Map<Long, CloneWorkspace> localWorkspaces = db.getWorkspaces();

        for(CloneWorkspace w: remoteWorkspaces){                
            if(localWorkspaces.containsKey(w.getId())){
                localWorkspaces.get(w.getId()).setRemoteRevision(w.getRemoteRevision());
            }else{
                localWorkspaces.put(w.getId(), w);                    
            }

            //save changes
            localWorkspaces.get(w.getId()).merge();
        }

        if(callbackListener != null){
            callbackListener.actionCompleted(true);
        }
        
        return localWorkspaces;
    }
    
    public static Map<Long, CloneWorkspace> InitializeWorkspaces(Profile profile) 
        throws InitializationException{
        
        return InitializeWorkspaces(profile, null);
        
    }
}
