package com.stacksync.desktop.config.profile;

import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.exceptions.ConfigException;
import java.util.UUID;

public class Account implements Configurable {
    
    private UUID id;
    private String email;
    private String password;
    private Long quotaLimit;
    private Long quotaUsed;
    
    public Account() {
        this(null);
    }
    
    public Account(UUID id) {
        this(id, null);
    }
    
    public Account(UUID id, String email) {
        this(id, email, null);
    }
    
    public Account(UUID id, String email, String password) {
        this(id, email, password, null, null);
    }
    
    public Account(UUID id, String email, String password, Long quotaLimit, Long quotaUsed) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.quotaLimit = quotaLimit;
        this.quotaUsed = quotaUsed;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getQuota() {
        return quotaLimit;
    }

    public void setQuota(Long quota) {
        this.quotaLimit = quota;
    }

    public Long getQuotaUsed() {
        return quotaUsed;
    }

    public void setQuotaUsed(Long quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        if (node == null) {
            return;
        }
        
        try {
            id = UUID.fromString(node.getProperty("id"));
            email = node.getProperty("email");
            password = node.getProperty("password");
            
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("id", id);
        node.setProperty("email", email);
        node.setProperty("password", password);
    }
    
}
