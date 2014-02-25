package com.stacksync.desktop.config.profile;

import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.exceptions.ConfigException;

public class Account implements Configurable {
    
    private String id;
    private String email;
    private String password;
    private Long quota;
    private Long quotaUsed;
    
    public Account() {
        this(null);
    }
    
    public Account(String id) {
        this(id, null);
    }
    
    public Account(String id, String email) {
        this(id, email, null);
    }
    
    public Account(String id, String email, String password) {
        this(id, email, password, null, null);
    }
    
    public Account(String id, String email, String password, Long quota, Long quotaUsed) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.quota = quota;
        this.quotaUsed = quotaUsed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
        return quota;
    }

    public void setQuota(Long quota) {
        this.quota = quota;
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
            id = node.getProperty("id");
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
