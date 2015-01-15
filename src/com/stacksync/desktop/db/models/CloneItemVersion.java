package com.stacksync.desktop.db.models;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.apache.log4j.Logger;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.logging.RemoteLogs;

@Entity
@Cacheable(false)
public class CloneItemVersion extends PersistentObject implements Serializable, Cloneable {

    private static final Logger logger = Logger.getLogger(CloneItemVersion.class.getName());

    public enum Status { UNKNOWN, NEW, CHANGED, RENAMED, DELETED };

    public enum SyncStatus { UNKNOWN, LOCAL, SYNCING, UPTODATE, CONFLICT, REMOTE, UNSYNC };
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false) 
    private CloneItem item;
    
    @Column(name = "file_version", nullable = false)
    private long version;
    
    @Column(name = "checksum")
    private long checksum;
    
    // FILE PROPERTIES
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
        this.version = 1;
        this.chunks = new ArrayList<CloneChunk>();
        this.status = Status.UNKNOWN;
        this.syncStatus = SyncStatus.UNKNOWN;

        this.checksum = 0;
        
        this.serverUploadedAck = false;
        this.serverUploadedTime = null;
    }

    public CloneItemVersion(File file) {
        this();
        
        this.size = file.isDirectory() ? 0 : file.length();
        this.lastModified = new Date(file.lastModified());       
        
    }
    
    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

    public CloneItem getItem() {
        return item;
    }

    public void setItem(CloneItem item) {
        this.item = item;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.getId() != null ? this.getId().hashCode() : 0);
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

        if (other.getId() == null || this.getId() == null) {
            return false;
        }

        return other.getId().equals(this.getId()) && other.version == this.version;
    }

    @Override
    public String toString() {

        return "CloneFile[id=" + getId() + ", version=" + version + " checksum=" + checksum + 
                ", chunks=" + chunks.size() + ", status=" + status + ", syncStatus=" + syncStatus + "]";
    }
    
    /*public void deleteFromDB() {

        String queryStr = "DELETE from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version = :version";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItemVersion.class);
        
        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        config.getDatabase().getEntityManager().getTransaction().begin();
        query.executeUpdate();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }*/
}