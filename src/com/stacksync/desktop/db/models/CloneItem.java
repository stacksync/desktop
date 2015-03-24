package com.stacksync.desktop.db.models;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Random;
import javax.persistence.*;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import java.util.ArrayList;

@Entity
@Cacheable(false)
public class CloneItem extends PersistentObject implements Serializable, Cloneable {

    private static final Logger logger = Logger.getLogger(CloneItem.class.getName());
    
    private static final long serialVersionUID = 12314234L;
    
    @Id
    @Column(name = "file_id", nullable = false)
    private Long id;
    
    @Column(name = "latest_version")
    private long latestVersion;
    
    @Transient
    private Folder root;
    
    // FILE PROPERTIES
    @Column(name = "name", length = 1024)
    private String name;
    
    @Column(name = "is_folder")
    private boolean folder;
    
    @ManyToOne(cascade=CascadeType.REMOVE)
    @JoinColumns({
        @JoinColumn(name = "parent_file_id", referencedColumnName = "file_id"),
    })
    private CloneItem parent;
    
    @Column(name = "file_path", nullable = false)
    private String path;
   
    @OneToOne
    private CloneWorkspace workspace;
    
    @Column(name = "mimetype")
    private String mimetype;
    
    @Column(name="is_temp_id", nullable= false)
    private boolean usingTempId;
    
    @Column(name="is_workspace_root", nullable=false)
    private boolean workspaceRoot;
    
    @OneToMany (targetEntity = CloneItemVersion.class, cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CloneItemVersion> versions;

    public CloneItem() {
        this.id = new Random().nextLong();

        this.path = "(unknown)";
        this.mimetype = "unknown";
        this.name = "(unknown)";
        
        this.usingTempId = true;
        this.workspaceRoot = false; // By default
    }
    
    public CloneItem(Folder root) {
        this();
        this.root = root;
    }

    public CloneItem(Folder root, File file) {
        this(root);
        
        this.name = file.getName();
        this.path = "/" + FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        this.path = FileUtil.getFilePathCleaned(path);
        
        this.folder = file.isDirectory();       
              
        this.mimetype = FileUtil.getMimeType(file);
        this.workspaceRoot = false; // By default
        
    }

    public Folder getRoot() {
        return root;
    }

    public void setRoot(Folder root) {
        this.root = root;
    }

    public void setParent(CloneItem parent) {
        this.parent = parent;
    }

    public CloneItem getParent() {
        return parent;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isUsingTempId() {
        return usingTempId;
    }

    public void setUsingTempId(boolean usingTempId) {
        this.usingTempId = usingTempId;
    }

    public String getPath() {
        
        generatePath();
        path = FileUtil.getFilePathCleaned(path);
        return path;
    }
    
    public void generatePath() {

        if (parent == null) {
            // Check if I'm in the default wp
            DatabaseHelper db = DatabaseHelper.getInstance();
            CloneWorkspace defaultWorkspace = db.getDefaultWorkspace();
            if (defaultWorkspace.getId().equals(getWorkspace().getId())) {
                path = "/";
            } else if (isWorkspaceRoot()) {
                if (parent == null) {
                    // If I'm the root workspace and my parent is null means I'm in the
                    // default root folder (stacksync_folder)
                    path = "/";
                } else {
                    // Otherwise I'm in a subfolder of the default workspace
                    setPath();
                }
            } else {
                // Not in the default wp, I have to continue
                CloneItem rootCloneFile = db.getWorkspaceRoot(getWorkspace().getId());
                String parentPath = rootCloneFile.getPath();
                if (parentPath.equals("/")) {
                    path = parentPath+rootCloneFile.getName();
                } else {
                    path = parentPath+"/"+rootCloneFile.getName();
                }
            }
            
        } else {
            setPath();
        }
    }
    
    private void setPath() {
        String parentPath = parent.getPath();
        if (parentPath.equals("/")) {
            path = parentPath+parent.getName();
        } else {
            path = parentPath+"/"+parent.getName();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return String.format("file-%s", getName());
    }

    public String getRelativePath() {
        return FileUtil.getRelativePath(getRoot().getLocalFile(), getFile());
    }

    public String getAbsolutePath() {
        return getFile().getAbsolutePath();
    }

    public String getRelativeParentDirectory() {
        return FileUtil.getRelativeParentDirectory(getRoot().getLocalFile(), getFile());
    }

    public String getAbsoluteParentDirectory() {
        return FileUtil.getAbsoluteParentDirectory(getFile());
    }

    public File getFile() {
        return FileUtil.getCanonicalFile(new File(getRoot().getLocalFile() + File.separator + getPath() + File.separator + getName()));
    }

    public boolean isWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(boolean workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }
    
    public CloneWorkspace getWorkspace() {
        return this.workspace;
    }
    
    public void setWorkspace(CloneWorkspace workspace){
        this.workspace = workspace;
    }
    
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }
    
    public String getMimetype(){
        return this.mimetype;
    }

    public long getLatestVersionNumber() {
        return latestVersion;
    }

    public void setLatestVersionNumber(long latestVersion) {
        this.latestVersion = latestVersion;
    }

    public List<CloneItemVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<CloneItemVersion> versions) {
        this.versions = versions;
    }
    
    public void addVersion(CloneItemVersion version) {
        if (versions==null) {
            versions = new ArrayList<CloneItemVersion>();
        }
        
        versions.add(version);
        if (version.getVersion() > this.latestVersion) {
            this.latestVersion = version.getVersion();
        }
    }
    
    public CloneItemVersion getLatestVersion() {
        CloneItemVersion latest = null;
        for (CloneItemVersion version : versions) {
            if (version.getVersion() == getLatestVersionNumber()){
                latest = version;
                break;
            } 
        }
        return latest;
    }
    
    public CloneItemVersion getLastSyncedVersion() {
        CloneItemVersion lastSynced = null;
        long biggestVersion = 0;
        for (CloneItemVersion version : versions) {
            if (version.getVersion() > biggestVersion && version.getSyncStatus() != CloneItemVersion.SyncStatus.UNSYNC) {
                lastSynced = version;
                biggestVersion = version.getVersion();
            } 
        }
        return lastSynced;
    }
    
    public CloneItemVersion getVersion(long versionNum) {
        for (CloneItemVersion version : versions) {
            if (version.getVersion() == versionNum) {
                return version;
            } 
        }
        return null;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public Object clone() {
        try {
            CloneItem clone = (CloneItem) super.clone();

            clone.id = getId();
            clone.root = getRoot(); // POINTER; No Copy!
            clone.folder = isFolder();
            clone.path = getPath();
            clone.name = getName();
            clone.parent = getParent(); // POINTER
            clone.usingTempId = isUsingTempId();
            clone.setWorkspaceRoot(isWorkspaceRoot());
            
            return clone;
        } catch (Exception ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);

            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneItem)) {
            return false;
        }

        CloneItem other = (CloneItem) object;

        if (other.id == null || this.id == null) {
            return false;
        }

        return other.id.equals(this.id);
    }

    @Override
    public String toString() {

        return "CloneFile[id=" + id + ", name="  
                + name + ", workspace=" + workspace + "]";
    }

    public long getNewRandom() {
        return new Random().nextLong();
    }
    
    /*
    public ItemMetadata mapToItemMetadata() throws NullPointerException {
        ItemMetadata object = new ItemMetadata();

        if (isUsingTempId()) {
            object.setId(null);
            object.setTempId(getId());
        } else {
            object.setId(getId());
        }
        
        object.setVersion(getVersion());
        object.setModifiedAt(getLastModified());

        object.setStatus(getStatus().toString());
        object.setChecksum(getChecksum());
        object.setMimetype(getMimetype());
        
        object.setSize(getSize());
        object.setIsFolder(isFolder());
        object.setDeviceId(config.getDeviceId());
        
        object.setFilename(getName());

        // Parent
        if (getParent() != null) {
            object.setParentId(getParent().getId());
            object.setParentVersion(getParent().getVersion());           
        } else{
            object.setParentId(null);            
            object.setParentVersion(null);
        }
        
        List<String> chunksList = new ArrayList<String>();        
        for(CloneChunk chunk: getChunks()){
            chunksList.add(chunk.getChecksum());
        }
        
        object.setChunks(chunksList);        
        return object;
    }
    */
}