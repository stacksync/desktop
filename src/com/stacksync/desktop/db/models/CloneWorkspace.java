package com.stacksync.desktop.db.models;

import com.ast.cloudABE.kpabe.KPABESecretKey;
import com.google.gson.Gson;
import com.stacksync.commons.models.ABEWorkspace;
import com.stacksync.commons.models.Workspace;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.PersistentObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

@Entity
@IdClass(value = CloneWorkspacePk.class)
public class CloneWorkspace extends PersistentObject implements Serializable {
        
    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name="id", nullable=false)
    private String id;
    
    @Column(name="path_workspace", nullable=false)
    private String pathWorkspace;
    
    @Column(name="local_revision", nullable=false)
    private Integer localLastUpdate;
    
    @Column(name="remote_revision", nullable=false)
    private Integer remoteLastUpdate;
    
    @Column(name="swift_container")
    private String swiftContainer;
    
    @Column(name="swift_url")
    private String swiftStorageURL;
    
    @Column(name="owner", nullable=false)
    private String owner;
    
    @Column(name="parent_id")
    private Long parentId;
    
    @Column(name="name", nullable=false)
    private String name;
    
    @Column(name="encrypted", nullable=false)
    private boolean encrypted;

    @Column(name="abe_encrypted", nullable=false)
    private boolean abeEncrypted;
    
    @Column(name="password")
    private String password;
    
    @Column(name="is_default")
    private boolean defaultWorkspace;
            
    @Column(name="master_key")
    private byte[] masterKey;

    @Column(name="public_key")
    private byte[] publicKey;
    
    @Column(name="secret_key")
    private byte[] secretKey;
        
    @Column(name="access_structure")
    private String accessStructure;
    
    @Column(name="group_generator")
    private byte[] groupGenerator;
    
    @Column(name="is_approved")
    private boolean isApproved;
        
    @OneToMany
    private List<CloneFile> files;
    
    @ElementCollection
    @JoinTable(name="attributes_version", joinColumns=@JoinColumn(name="id"))
    @MapKeyColumn (name="name")
    @Column(name="version")
    Map<String, Long> attributesVersion = new HashMap<String, Long>(); // maps from attribute name to value

    public CloneWorkspace(){}
    
    public CloneWorkspace(Workspace r){
        this.id = r.getId().toString();
        this.name = r.getName();
        
        this.parentId = null;
        if (r.getParentItem() != null && r.getParentItem().getId() != null) {
            this.parentId = r.getParentItem().getId();
        }
        
        this.defaultWorkspace = !r.isShared();
        this.localLastUpdate = r.getLatestRevision();
        this.remoteLastUpdate = r.getLatestRevision();
        this.swiftContainer = r.getSwiftContainer();
        this.swiftStorageURL = r.getSwiftUrl();
        this.owner = r.getOwner().getId().toString();
        this.pathWorkspace = generatePath();
        this.encrypted = r.isEncrypted();
        this.abeEncrypted = false;

        if (!defaultWorkspace) {
            this.encrypted = r.isEncrypted();
            this.abeEncrypted = r.isAbeEncrypted();
        }
    }
    
    public CloneWorkspace(ABEWorkspace r){
        this.id = r.getId().toString();
        this.name = r.getName();
        
        this.parentId = null;
        if (r.getParentItem() != null && r.getParentItem().getId() != null) {
            this.parentId = r.getParentItem().getId();
        }
        
        this.defaultWorkspace = !r.isShared();
        this.localLastUpdate = r.getLatestRevision();
        this.remoteLastUpdate = r.getLatestRevision();
        this.swiftContainer = r.getSwiftContainer();
        this.swiftStorageURL = r.getSwiftUrl();
        this.owner = r.getOwner().getId().toString();
        this.pathWorkspace = generatePath();
        this.encrypted = false;
        this.abeEncrypted = true;

        if (!defaultWorkspace) {
            this.encrypted = r.isEncrypted();
            this.abeEncrypted = r.isAbeEncrypted();
        }
        
        if(((ABEWorkspace)r).getAccess_struct()!=null)
            this.accessStructure = new String(((ABEWorkspace)r).getAccess_struct()); 
        
        this.publicKey = r.getPublicKey();
        this.secretKey = r.getSecretKey();
        
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPathWorkspace(){
        this.pathWorkspace = generatePath();
        return pathWorkspace;
    }
    
    private String generatePath() {
        
        String path;
        
        if (this.parentId != null) {
            CloneFile parent = DatabaseHelper.getInstance().getFileOrFolder(this.parentId);
            path = parent.getPath();
            if (!path.endsWith("/")) {
                path += "/";
            }
            path += parent.getName()+ "/" + this.name;
        } else {
            if (isDefaultWorkspace()) {
                path = "/";
            } else {
                path = "/" + this.name;
            }
        }
        
        return path;
    }
    
    public void setPathWorkspace(String pathWorkspace){
        this.pathWorkspace = pathWorkspace;
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

    public String getSwiftContainer() {
        return swiftContainer;
    }

    public void setSwiftContainer(String swiftContainer) {
        this.swiftContainer = swiftContainer;
    }

    public String getSwiftStorageURL() {
        return swiftStorageURL;
    }

    public void setSwiftStorageURL(String swiftStorageURL) {
        this.swiftStorageURL = swiftStorageURL;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
    
    public boolean isAbeEncrypted() {
        return abeEncrypted;
    }

    public void setAbeEncrypted(boolean abeEncrypted) {
        this.abeEncrypted = abeEncrypted;
    }

    public byte[] getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(byte[] masterKey) {
        this.masterKey = masterKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] privateKey) {
        this.secretKey = privateKey;
    }

    public String getAccessStructure() {
        return accessStructure;
    }

    public void setAccessStructure(String accessStructure) {
        this.accessStructure = accessStructure;
    }

    public boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }
    
    public boolean isDefaultWorkspace() {
        return defaultWorkspace;
    }

    public byte[] getGroupGenerator() {
        return groupGenerator;
    }

    public void setGroupGenerator(byte[] groupGenerator) {
        this.groupGenerator = groupGenerator;
    }
    
    public void setDefaultWorkspace(boolean defaultWorkspace) {
        this.defaultWorkspace = defaultWorkspace;
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
    
    public Map<String, Long> getAttributesVesion() {
        return attributesVersion;
    }

    public void setAttributesVersion(Map<String, Long> attributes) {
        this.attributesVersion = attributes;
    }
    
    @Override
    public CloneWorkspace clone() {
        CloneWorkspace workspace = new CloneWorkspace();
        workspace.setId(getId());
        workspace.setName(getName());
        workspace.setOwner(getOwner());
        workspace.setParentId(getParentId());
        workspace.setPathWorkspace(getPathWorkspace());
        workspace.setRemoteRevision(getRemoteRevision());
        workspace.setLocalLastUpdate(getLocalLastUpdate());
        workspace.setSwiftContainer(getSwiftContainer());
        workspace.setSwiftStorageURL(getSwiftStorageURL());
        workspace.setPassword(getPassword());
        workspace.setEncrypted(isEncrypted());
        workspace.setAbeEncrypted(isAbeEncrypted());
        workspace.setDefaultWorkspace(isDefaultWorkspace());
        workspace.setPublicKey(getPublicKey());
        workspace.setMasterKey(getMasterKey());
        workspace.setAccessStructure(getAccessStructure());
        workspace.setIsApproved(getIsApproved());
        workspace.setSecretKey(getSecretKey());
        workspace.setGroupGenerator(getGroupGenerator());
        
        HashMap<String,Long> newAttributesVersion = new HashMap<String,Long>();
        
        for(String attribute:getAttributesVesion().keySet()){
            newAttributesVersion.put(attribute, getAttributesVesion().get(attribute));
        }
        
        workspace.setAttributesVersion(newAttributesVersion);
        
        return workspace;
    }

}
