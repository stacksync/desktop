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
package com.stacksync.desktop.connection.plugins.rest;

import java.util.ResourceBundle;
import org.jets3t.service.security.ProviderCredentials;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author oubou68, pheckel
 */
public abstract class RestConnection implements Connection {
    protected String accessKey;
    protected String secretKey; 
    protected String bucket;    
    protected ProviderCredentials credentials;
    protected ResourceBundle resourceBundle;

    public RestConnection() {
         resourceBundle = Config.getInstance().getResourceBundle();
    }        
    
    public String getAccessKey() {
        return accessKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public ProviderCredentials getCredentials() {
        if (credentials == null) {
            credentials = createCredentials();
        }
        
        return credentials;
    }        
    
    protected abstract ProviderCredentials createCredentials();        

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        accessKey = node.getProperty("accesskey");
        secretKey = node.getProperty("secretkey");
        bucket = node.getProperty("bucket");

        if (accessKey == null || secretKey == null || bucket == null) {
                throw new ConfigException("Rest connection properties must at least contain the parameters 'accesskey', 'secretkey' and 'bucket'.");
            }

        // Init
        credentials = createCredentials();
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", getPluginInfo().getId());
        node.setProperty("accesskey", accessKey);
        node.setProperty("secretkey", secretKey);
        node.setProperty("bucket", bucket);
    }
    
    @Override
    public String toString() {
        return RestConnection.class.getSimpleName()
        + "[" + resourceBundle.getString("bucket") + "=" + bucket + "]";
    }	      
}
