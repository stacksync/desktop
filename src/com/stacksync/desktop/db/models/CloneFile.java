/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.db.models;

import com.stacksync.commons.models.ABEItemMetadata;
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
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.PersistentObject;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.SyncMetadata;
import com.stacksync.desktop.db.DatabaseHelper;
import java.util.UUID;

@Entity
@Cacheable(false)
@IdClass(value = CloneFilePk.class)
public class CloneFile extends PersistentObject implements Serializable, Cloneable {

    private static final Logger logger = Logger.getLogger(CloneFile.class.getName());
    private static final Config config = Config.getInstance();

    private static final long serialVersionUID = 12314234L;

    /**
     * <ul> <li>UNKNOWN <li>NEW: New file <lI>CHANGED: The file contents have
     * changed. At least one chunk differs. <li>RENAMED: The file path or name
     * has changed. <li>MERGED: The file history has been merged to a different
     * file. </ul>
     */
    public enum Status {

        UNKNOWN, NEW, CHANGED, RENAMED, DELETED
    };

    /**
     * LOCAL: The file entry hasn't been propagated to the server yet IN_UPDATE:
     * The file entry should be included in the update-file, but not (yet) in
     * the base file IN_BASE: The file entry should be included in the base-file
     * (= complete DB dump)
     */
    public enum SyncStatus {

        UNKNOWN, LOCAL, SYNCING, UPTODATE, CONFLICT, REMOTE, UNSYNC
    };

    /**
     * versionId of the root file; identifies the history of a file
     */
    @Id
    @Column(name = "file_id", nullable = false)
    private Long id;

    @Id
    @Column(name = "file_version", nullable = false)
    private long version;

    @Column(name = "checksum")
    private long checksum;

    @Transient
    private Profile profile;

    @Transient
    private Folder root;

    @Transient
    private byte[] symmetricKey;

    @Transient
    private List<ABEMetaComponent> abeComponents;

    @Column(name = "enc_symmetric_key")
    private byte[] cipherSymKey;

    // FILE PROPERTIES
    @Column(name = "is_folder")
    private boolean folder;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumns({
        @JoinColumn(name = "parent_file_id", referencedColumnName = "file_id"),
        @JoinColumn(name = "parent_file_version", referencedColumnName = "file_version"),})
    private CloneFile parent;

    @Column(name = "file_path", nullable = false)
    private String path;

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

    @Column(name = "server_uploaded_ack")
    private boolean serverUploadedAck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "server_uploaded_time")
    private Date serverUploadedTime;

    @Column(name = "is_temp_id", nullable = false)
    private boolean usingTempId;

    @Column(name = "is_workspace_root", nullable = false)
    private boolean workspaceRoot;

    public CloneFile() {
        this.id = new Random().nextLong();
        this.version = 1;
        this.chunks = new ArrayList<CloneChunk>();
        this.status = Status.UNKNOWN;
        this.syncStatus = SyncStatus.UNKNOWN;

        this.checksum = 0;
        this.name = "(unknown)";
        this.path = "(unknown)";
        this.mimetype = "unknown";

        this.serverUploadedAck = false;
        this.serverUploadedTime = null;
        this.usingTempId = true;
        this.workspaceRoot = false; // By default
    }

    public CloneFile(Folder root, File file) {
        this();

        // Set account
        this.profile = root.getProfile();
        this.root = root;

        this.name = file.getName();
        this.path = "/" + FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        this.path = FileUtil.getFilePathCleaned(path);

        this.size = file.isDirectory() ? 0 : file.length();
        this.lastModified = new Date(file.lastModified());
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

    public void setParent(CloneFile parent) {
        this.parent = parent;
    }

    public CloneFile getParent() {
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
                CloneFile rootCloneFile = db.getWorkspaceRoot(getWorkspace().getId());
                String parentPath = rootCloneFile.getPath();
                if (parentPath.equals("/")) {
                    path = parentPath + rootCloneFile.getName();
                } else {
                    path = parentPath + "/" + rootCloneFile.getName();
                }
            }

        } else {
            setPath();
        }
    }

    private void setPath() {
        String parentPath = parent.getPath();
        if (parentPath.equals("/")) {
            path = parentPath + parent.getName();
        } else {
            path = parentPath + "/" + parent.getName();
        }
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

    public boolean isWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(boolean workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public byte[] getCipherSymKey() {
        return cipherSymKey;
    }

    public void setCipherSymKey(byte[] cipherSymKey) {
        this.cipherSymKey = cipherSymKey;
    }

    public byte[] getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(byte[] symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public List<ABEMetaComponent> getAbeComponents() {
        return abeComponents;
    }

    public void setAbeComponents(List<ABEMetaComponent> abeComponents) {
        this.abeComponents = abeComponents;
    }
    
    public ArrayList<com.stacksync.commons.models.ABEMetaComponent> getCommonAbeComponents() {
        ArrayList<com.stacksync.commons.models.ABEMetaComponent> components = 
                new ArrayList<com.stacksync.commons.models.ABEMetaComponent>();
        for(ABEMetaComponent meta : getAbeComponents()) {
            components.add(new com.stacksync.commons.models.ABEMetaComponent(
                    meta.getId(), meta.getAttribute(), meta.getEncryptedPKComponent(), meta.getVersion()));
        }
        return components;
    }

    public CloneFile getPreviousVersion() {
        // If we are the first, there are no others
        if (getVersion() == 1) {
            return null;
        }

        List<CloneFile> pV = getPreviousVersions();

        if (pV.isEmpty()) {
            return null;
        }

        return pV.get(pV.size() - 1);
    }

    public List<CloneFile> getPreviousVersions() {
        // If we are the first, there are no others
        if (getVersion() == 1) {
            return new ArrayList<CloneFile>();
        }

        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version < :version "
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");

        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        List<CloneFile> list = query.getResultList();
        return list;
    }

    public List<CloneFile> getNextVersions() {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version > :version "
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");

        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        List<CloneFile> list = query.getResultList();
        return list;
    }

    public List<CloneFile> getVersionHistory() {
        List<CloneFile> versions = new ArrayList<CloneFile>();

        versions.addAll(getPreviousVersions());
        versions.add(this);
        versions.addAll(getNextVersions());

        return versions;
    }

    public CloneFile getFirstVersion() {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id "
                // + "   and c.version = 1");
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");

        query.setMaxResults(1);
        query.setParameter("id", getId());

        try {
            return (CloneFile) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }

    // TODO optimize this!!! It returns a list with ALL the entries where the
    // parent is this file. This is incorrect since could be files moved to
    // other folders in further versions.
    public List<CloneFile> getChildren() {

        String queryStr = "select c from CloneFile c where "
                + "     c.parent = :parent";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");

        query.setParameter("parent", this);
        List<CloneFile> list = query.getResultList();

        /*for(CloneFile cf: list){
         config.getDatabase().getEntityManager().refresh(cf);
         }*/
        return list;
    }

    public CloneFile getLastVersion() {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id"
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");

        query.setMaxResults(1);
        query.setParameter("id", getId());

        try {
            return (CloneFile) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }

    public CloneFile getLastSyncedVersion() {

        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version < :version and "
                + "     c.syncStatus = :syncStatus "
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        query.setMaxResults(1);

        query.setParameter("id", getId());
        query.setParameter("version", getVersion());
        query.setParameter("syncStatus", SyncStatus.UPTODATE);

        try {
            return (CloneFile) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }

    public void deleteHigherVersion() {

        String queryStr = "DELETE from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version > :version";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);

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

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);

        query.setParameter("id", getId());
        query.setParameter("version", getVersion());

        config.getDatabase().getEntityManager().getTransaction().begin();
        query.executeUpdate();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }

    public Status getStatus() {
        return status;
    }

    public boolean getServerUploadedAck() {
        return this.serverUploadedAck;
    }

    public void setServerUploadedAck(boolean serverUploadedAck) {
        this.serverUploadedAck = serverUploadedAck;
    }

    public void setServerUploadedTime(Date serverUploadedTime) {
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
            CloneFile clone = (CloneFile) super.clone();

            clone.id = getId();
            clone.checksum = getChecksum();
            clone.lastModified = new Date(getLastModified().getTime());
            clone.profile = getProfile(); // POINTER; No Copy!
            clone.root = getRoot(); // POINTER; No Copy!
            clone.folder = isFolder();
            clone.path = getPath();
            clone.name = getName();
            clone.size = getSize();
            clone.chunks = getChunks(); // POINTER; No Copy!
            clone.status = getStatus(); //TODO: is this ok?
            clone.syncStatus = getSyncStatus(); //TODO: is this ok?
            clone.parent = getParent(); // POINTER      

            clone.serverUploadedAck = false;
            clone.serverUploadedTime = null;
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
        if (!(object instanceof CloneFile)) {
            return false;
        }

        CloneFile other = (CloneFile) object;

        if (other.id == null || this.id == null) {
            return false;
        }

        return other.id.equals(this.id) && other.version == this.version;
    }

    @Override
    public String toString() {

        return "CloneFile[id=" + id + ", version=" + version + ", name="
                + name + " checksum=" + checksum + ", chunks=" + chunks.size()
                + ", status=" + status + ", syncStatus=" + syncStatus + ", workspace="
                + workspace + "]";
    }

    public long getNewRandom() {
        return new Random().nextLong();
    }

    public CloneWorkspace getWorkspace() {
        return this.workspace;
    }

    public void setWorkspace(CloneWorkspace workspace) {
        this.workspace = workspace;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getMimetype() {
        return this.mimetype;
    }

    public SyncMetadata mapToSyncMetadata() throws NullPointerException {
        ItemMetadata object = new ItemMetadata();

        if (isUsingTempId()) {
            object.setId(null);
            object.setTempId(getId());
        } else {
            object.setId(getId());
        }

        object.setWorkspaceId(UUID.fromString(getWorkspace().getId()));

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
        } else {
            object.setParentId(null);
            object.setParentVersion(null);
        }

        List<String> chunksList = new ArrayList<String>();
        for (CloneChunk chunk : getChunks()) {
            chunksList.add(chunk.getChecksum());
        }

        object.setChunks(chunksList);

        // Return ABEItemMetadata object if workspace implements ABE encryption
        if (getWorkspace().isAbeEncrypted()) {
            ABEItemMetadata abeObject = new ABEItemMetadata(object, getCommonAbeComponents(), getCipherSymKey());
            abeObject.setTempId(object.getTempId());
            abeObject.setWorkspaceId(object.getWorkspaceId());
            abeObject.setChunks(object.getChunks());
            return abeObject;
        }
        // Return ItemMetadata object otherwise
        return object;
    }
}
