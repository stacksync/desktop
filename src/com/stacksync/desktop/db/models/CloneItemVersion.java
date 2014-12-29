package com.stacksync.desktop.db.models;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.persistence.*;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.logging.RemoteLogs;

@Entity
@Cacheable(false)
@IdClass(value = CloneFilePk.class)
public class CloneItemVersion extends PersistentObject implements Serializable, Cloneable {

    private static final Logger logger = Logger.getLogger(CloneItemVersion.class.getName());
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
    @ManyToOne(cascade=CascadeType.REMOVE)
    @JoinColumns({
        @JoinColumn(name = "file_id", referencedColumnName = "file_id"),
    })
    @Column(name = "file_id", nullable = false)
    private Long id;
    
    @Id
    @Column(name = "file_version", nullable = false)
    private long version;
    
    @Column(name = "checksum")
    private long checksum;
    
    // FILE PROPERTIES
    @Column(name = "name", length = 1024)
    private String name;
    
    @Column(name = "file_size")
    private long size;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified")
    private Date lastModified;
    
    @ManyToMany
    @OrderColumn
    private List<CloneChunk> chunks;
        
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status")
    private SyncStatus syncStatus;
    
    @Column(name="server_uploaded_ack")
    private boolean serverUploadedAck;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="server_uploaded_time")
    private Date serverUploadedTime;

    public CloneItemVersion() {
        this.id = new Random().nextLong();
        this.version = 1;
        this.chunks = new ArrayList<CloneChunk>();
        this.status = Status.UNKNOWN;
        this.syncStatus = SyncStatus.UNKNOWN;

        this.checksum = 0;
        this.name = "(unknown)";
        
        this.serverUploadedAck = false;
        this.serverUploadedTime = null;
    }

    public CloneItemVersion(Folder root, File file) {
        this();

        this.name = file.getName();
        
        this.size = file.isDirectory() ? 0 : file.length();
        this.lastModified = new Date(file.lastModified());       
        
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return String.format("file-%s-%020d", getChecksum(), getLastModified().getTime());
    }

    public long getSize() {
        return size;
    }

    public void setSize(long fileSize) {
        this.size = fileSize;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public CloneItemVersion getPreviousVersion() {
        // If we are the first, there are no others
        if (getVersion() == 1) {
            return null;
        }

        List<CloneItemVersion> pV = getPreviousVersions();

        if (pV.isEmpty()) {
            return null;
        }

        return pV.get(pV.size() - 1);
    }

    public List<CloneItemVersion> getPreviousVersions() {
        // If we are the first, there are no others
        if (getVersion() == 1) {
            return new ArrayList<CloneItemVersion>();
        }

        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version < :version "
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItemVersion.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        List<CloneItemVersion> list = query.getResultList();
        return list;
    }
    
    public List<CloneItemVersion> getNextVersions() {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version > :version "
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItemVersion.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("id", getId());
        query.setParameter("version", getVersion());
        
        List<CloneItemVersion> list = query.getResultList();
        return list;
    }
    
    public void deleteHigherVersion() {

        String queryStr = "DELETE from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version > :version";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItemVersion.class);
        
        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        config.getDatabase().getEntityManager().getTransaction().begin();
        query.executeUpdate();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }
    
    public void deleteFromDB() {

        String queryStr = "DELETE from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version = :version";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItemVersion.class);
        
        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        config.getDatabase().getEntityManager().getTransaction().begin();
        query.executeUpdate();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }

    public Status getStatus() {
        return status;
    }
    
    public boolean getServerUploadedAck(){
        return this.serverUploadedAck;
    }
    
    public void setServerUploadedAck(boolean serverUploadedAck) {
        this.serverUploadedAck = serverUploadedAck;
    }
    
    public void setServerUploadedTime(Date serverUploadedTime){
        this.serverUploadedTime = serverUploadedTime;
    }
    
    public Date getServerUploadedTime() {
        return this.serverUploadedTime;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }

    public List<CloneChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<CloneChunk> chunks) {
        this.chunks = new ArrayList<CloneChunk>(chunks);
    }

    public void addChunk(CloneChunk chunk) {
        chunks.add(chunk);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        hash += version;
        return hash;
    }

    @Override
    public Object clone() {
        try {
            CloneItemVersion clone = (CloneItemVersion) super.clone();

            clone.id = getId();
            clone.checksum = getChecksum();
            clone.lastModified = new Date(getLastModified().getTime());
            clone.name = getName();
            clone.size = getSize();
            clone.chunks = getChunks(); // POINTER; No Copy!
            clone.status = getStatus(); //TODO: is this ok?
            clone.syncStatus = getSyncStatus(); //TODO: is this ok?    
                      
            clone.serverUploadedAck = false;
            clone.serverUploadedTime = null;
            
            return clone;
        } catch (Exception ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);

            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CloneItemVersion)) {
            return false;
        }

        CloneItemVersion other = (CloneItemVersion) object;

        if (other.id == null || this.id == null) {
            return false;
        }

        return other.id.equals(this.id) && other.version == this.version;
    }

    @Override
    public String toString() {

        return "CloneFile[id=" + id + ", version=" + version + ", name="  
                + name + " checksum=" + checksum + ", chunks=" + chunks.size() 
                + ", status=" + status + ", syncStatus=" + syncStatus + "]";
    }

    public long getNewRandom() {
        return new Random().nextLong();
    }
    
}