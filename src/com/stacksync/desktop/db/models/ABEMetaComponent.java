package com.stacksync.desktop.db.models;

import com.stacksync.desktop.db.PersistentObject;
import java.io.Serializable;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

/**
 *
 * @author javigd
 */
@Entity
@Cacheable(false)
public class ABEMetaComponent extends PersistentObject implements Serializable, Cloneable {

    private static final long serialVersionUID = 3232299912L;

    @Id
    @Column(name = "abe_component_id", nullable = false)
    private Long id;
    
    @ManyToOne(cascade=CascadeType.REMOVE)
    @JoinColumns({
        @JoinColumn(name = "enc_file_id", referencedColumnName = "file_id"),
        @JoinColumn(name = "enc_file_version", referencedColumnName = "file_version"),
    })
    private CloneFile file;

    @Column(name = "attribute_id", nullable = false)
    private String attribute;

    @Column(name = "encrypted_pk_component", nullable = false)
    private String encryptedPKComponent;

    @Column(name = "abe_component_version", nullable = false)
    private Long version;

    public ABEMetaComponent() {
        this.id = null;
    }

    public ABEMetaComponent(Long id, String attribute, String encryptedPKComponent, Long version) {
        this.id = id;
        this.attribute = attribute;
        this.encryptedPKComponent = encryptedPKComponent;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEncryptedPKComponent() {
        return encryptedPKComponent;
    }

    public void setEncryptedPKComponent(String encryptedPKComponent) {
        this.encryptedPKComponent = encryptedPKComponent;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    
    public CloneFile getFile() {
        return file;
    }

    public void setFile(CloneFile file) {
        this.file = file;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public boolean isValid() {
        boolean valid = true;
        if (this.attribute == null || this.encryptedPKComponent == null || this.version == null) {
            valid = false;
        }
        return valid;
    }

    @Override
    public String toString() {

        String format = "ItemMetadata: {id=%s, attribute=%s, pk component=%s}";
        String result = String.format(format, id, attribute, encryptedPKComponent);

        return result;
    }
}
