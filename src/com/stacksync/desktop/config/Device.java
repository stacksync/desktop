package com.stacksync.desktop.config;

import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import java.util.UUID;

public class Device implements Configurable {
    
    private UUID id;
    private String name;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public static void initializeDevice(Profile profile) throws InitializationException {
        
        
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        if (node == null) {
            return;
        }
        
        setName(node.getProperty("name").replace("-", "_"));
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("name", name);
    }
    
    @Override
    public String toString() {
        return getName()+"_"+getId();
    }
    
}
