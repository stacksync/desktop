package com.stacksync.desktop.db.models;

import com.stacksync.desktop.db.PersistentObject;
import java.io.Serializable;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 *
 * @author javigd
 */
@Entity
@Cacheable(false)
public class Attribute extends PersistentObject implements Serializable, Cloneable {
    
    @Id
    @Column(name = "attribute_id", nullable = false)
    private Long id;
    
    @Column(name = "attribute_name")
    private String name;
    
    @Column(name = "latest_version", nullable = false)
    private Long latestVersion;
    
    @Column(name = "public_key_component", nullable = false)
    private String publicKeyComponent;

    public Attribute() {
        this.id = null;
    }
    
    public Attribute(Long id, String name, Long latestVersion, String publicKeyComponent) {
        this.id = id;
        this.name = name;
        this.latestVersion = latestVersion;
        this.publicKeyComponent = publicKeyComponent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(Long latestVersion) {
        this.latestVersion = latestVersion;
    }

    public String getPublicKeyComponent() {
        return publicKeyComponent;
    }

    public void setPublicKeyComponent(String publicKeyComponent) {
        this.publicKeyComponent = publicKeyComponent;
    }

    public boolean isValid() {
        boolean valid = true;
        if (this.name == null || this.latestVersion == null || this.publicKeyComponent == null) {
            valid = false;
        }
        return valid;
    }

}
