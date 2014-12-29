package com.stacksync.desktop.db.models;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.persistence.*;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.db.DatabaseHelper;

@Entity
@Cacheable(false)
@IdClass(value = CloneFilePk.class)
public class CloneItem extends PersistentObject implements Serializable, Cloneable {

    private static final Logger logger = Logger.getLogger(CloneItem.class.getName());
    private static final Config config = Config.getInstance();
    
    private static final long serialVersionUID = 12314234L;

    /**
     * <ul> <li>UNKNOWN <li>NEW: New file <lI>CHANGED: The file contents have
     * changed. At least one chunk differs. <li>RENAMED: The file path or name
     * has changed. <li>MERGED: The file history has been merged to a different
     * file. </ul>
     */
    public enum Status { UNKNOWN, NEW, CHANGED, RENAMED, DELETED };

    /**
     * LOCAL: The file entry hasn't been propagated to the server yet IN_UPDATE:
     * The file entry should be included in the update-file, but not (yet) in
     * the base file IN_BASE: The file entry should be included in the base-file
     * (= complete DB dump)
     */
    public enum SyncStatus { UNKNOWN, LOCAL, SYNCING, UPTODATE, CONFLICT, REMOTE, UNSYNC };
    
    /**
     * versionId of the root file; identifies the history of a file
     */
    @Id
    @Column(name = "file_id", nullable = false)
    private Long id;
    
    @Column(name = "latest_version")
    private long latest_version;
    
    @Transient
    private Profile profile;
    
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
        
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status")
    private SyncStatus syncStatus;
    
    @Column(name = "mimetype")
    private String mimetype;
    
    @Column(name="is_temp_id", nullable= false)
    private boolean usingTempId;
    
    @Column(name="is_workspace_root", nullable=false)
    private boolean workspaceRoot;

    public CloneItem() {
        this.id = new Random().nextLong();
        this.status = Status.UNKNOWN;
        this.syncStatus = SyncStatus.UNKNOWN;

        this.path = "(unknown)";
        this.mimetype = "unknown";
        this.name = "(unknown)";
        
        this.usingTempId = true;
        this.workspaceRoot = false; // By default
    }

    public CloneItem(Folder root, File file) {
        this();

        // Set account
        this.profile = root.getProfile();
        this.root = root;
        
        this.name = file.getName();
        this.path = "/" + FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        this.path = FileUtil.getFilePathCleaned(path);
        
        this.folder = file.isDirectory();       
              
        this.mimetype = FileUtil.getMimeType(file);
        this.workspaceRoot = false; // By default
        
    }

    public Folder getRoot() {
        if (root == null) {
            root = getProfile().getFolder();
        }

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

    public Profile getProfile() {
        if (profile == null) {
            profile = config.getProfile();
        }

        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
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

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
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

    /**
     * Get relative path to the root dir.
     */
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

    public List<CloneItem> getVersionHistory() {
        List<CloneItem> versions = new ArrayList<CloneItem>();

        versions.addAll(getPreviousVersions());
        versions.add(this);
        versions.addAll(getNextVersions());

        return versions;
    }

    public CloneItem getFirstVersion() {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id "
               // + "   and c.version = 1");
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setMaxResults(1);
        query.setParameter("id", getId());

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }
    
    // TODO optimize this!!! It returns a list with ALL the entries where the
    // parent is this file. This is incorrect since could be files moved to
    // other folders in further versions.
    public List<CloneItem> getChildren() {
        
        String queryStr = "select c from CloneFile c where "
                + "     c.parent = :parent";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("parent", this);
        List<CloneItem> list = query.getResultList();
        
        /*for(CloneFile cf: list){
            config.getDatabase().getEntityManager().refresh(cf);
        }*/
        return list;
    }

    public CloneItem getLastVersion() {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id"
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setMaxResults(1);
        query.setParameter("id", getId());

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }
    
    public CloneItem getLastSyncedVersion() {

        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version < :version and "
                + "     c.syncStatus = :syncStatus "
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");    
        query.setMaxResults(1);
        
        query.setParameter("id", getId());
        query.setParameter("version", getVersion());
        query.setParameter("syncStatus", SyncStatus.UPTODATE);

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }

    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
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
            clone.profile = getProfile(); // POINTER; No Copy!
            clone.root = getRoot(); // POINTER; No Copy!
            clone.folder = isFolder();
            clone.path = getPath();
            clone.name = getName();
            clone.status = getStatus(); //TODO: is this ok?
            clone.syncStatus = getSyncStatus(); //TODO: is this ok?
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
                + name + ", status=" + status + ", syncStatus=" + syncStatus + ", workspace=" 
                + workspace + "]";
    }

    public long getNewRandom() {
        return new Random().nextLong();
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