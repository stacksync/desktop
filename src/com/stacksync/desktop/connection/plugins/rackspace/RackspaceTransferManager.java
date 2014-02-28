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
package com.stacksync.desktop.connection.plugins.rackspace;

import com.rackspacecloud.client.cloudfiles.FilesClient;
import com.rackspacecloud.client.cloudfiles.FilesObject;
import com.rackspacecloud.client.cloudfiles.expections.OverQuotaException;
import com.rackspacecloud.client.cloudfiles.expections.UnauthorizeException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.LocalFileNotFoundException;
import com.stacksync.desktop.exceptions.RemoteFileNotFoundException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.exceptions.StorageQuotaExcedeedException;
import com.stacksync.desktop.exceptions.StorageUnauthorizeException;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.FileUtil;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 *
 * @author oubou68, pheckel
 */
public class RackspaceTransferManager extends AbstractTransferManager {

    private final Logger logger = Logger.getLogger(RackspaceTransferManager.class.getName());
    private final int CONNECTION_TIMEOUT = 60 * 1000;
    
    private String AUTH_URL;    
    private FilesClient client;

    public RackspaceTransferManager(RackspaceConnection connection) {
        super(connection);

        AUTH_URL = connection.getAuthUrl();
        client = new FilesClient(connection.getUsername(), connection.getApiKey(), AUTH_URL, null, CONNECTION_TIMEOUT);
    }

    @Override
    public RackspaceConnection getConnection() {
        return (RackspaceConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        
        if (client.isLoggedin()) {
            return;
        }
        
        try {
            client.loginKeystone();
        } catch (UnknownHostException ex) {
            logger.error(ex);
            throw new StorageConnectException(ex);
        } catch (SocketException ex) {
            logger.error(ex);
            throw new StorageConnectException(ex);
        } catch (IOException ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
            throw new StorageConnectException(ex);
        } catch (UnauthorizeException ex) {
            logger.error(ex);
            throw new StorageConnectException(ex);
        }

    }
    
    public boolean isConnected(){
        return client.isLoggedin();
    }

    @Override
    public void disconnect() throws StorageException {
        // Fressen.
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();
        File tempFile = null;
        InputStream is = null;
        
        try {
            is = client.getObjectAsStream(getConnection().getContainer(), remoteFile.getName());

            // Save to temp file
            tempFile = config.getCache().createTempFile(remoteFile.getName());
            FileUtil.writeFile(is, tempFile);

            FileUtil.copy(tempFile, localFile);

        } catch (Exception ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
            throw new StorageException("Unable to download file '" + remoteFile.getName(), ex);
        } finally {
            try {
                if (is != null){
                    is.close();
                }
            } catch (IOException ex) {
                logger.error("I/O Excdeption: ", ex);
                RemoteLogs.getInstance().sendLog(ex);
            } 
            
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException, StorageQuotaExcedeedException {
        connect();

        // Check if exists
        Collection<RemoteFile> obj = list(remoteFile.getName()).values();

        if (obj != null && !obj.isEmpty()) {
            return;
        }
        
        try {
            // Upload
            client.storeObjectAs(getConnection().getContainer(), localFile, "application/x-Stacksync", remoteFile.getName());
        } catch (OverQuotaException ex) {
            logger.error("Quota limit exceeded. Could not upload file "+localFile.getName(), ex);
            throw new StorageQuotaExcedeedException(ex);
        } catch (Exception ex) {
            logger.error(ex);
            throw new StorageException(ex);
        }

    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            client.deleteObject(getConnection().getContainer(), remoteFile.getName());
        } catch (Exception ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        return list("");
    }

    @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
        connect();

        try {
            List<FilesObject> objects = client.listObjectsStartingWith(getConnection().getContainer(), namePrefix, null, -1, null);
            Map<String, RemoteFile> list = new HashMap<String, RemoteFile>();
                      
            while(objects.size() > 0){
                for (FilesObject obj : objects) {
                    list.put(obj.getName(), new RemoteFile(obj.getName(), obj.getSize(), obj));
                }
                
                objects = client.listObjectsStartingWith(getConnection().getContainer(), namePrefix, null, -1, objects.get(objects.size() - 1 ).getName());
            }
            
            return list;

        } catch (Exception ex) {
            logger.error(ex);
            //LogConfig.sendLog();
            throw new StorageException(ex);
        }
    }

    public void createContainer(String containerName) throws StorageException  {
        
        try {
            connect();
            if(!client.containerExists(containerName)){
                client.createContainer(containerName);
            }
        } catch (StorageConnectException ex) {
            
            String host = "";
            try {
                URL url = new URL(AUTH_URL);
                host = url.getHost();
            } catch (MalformedURLException ex1) {
                logger.warn("Incorrect URL: ", ex1);
            }
            
            // This error is from connect() function
            // It is necessary to differenciate between unauthorize, no connection or others...
            String message = ex.getCause().getMessage();
            if (message.contains("Incorrect")) {
                // Unauthorize
                throw new StorageUnauthorizeException("Incorrect user or password.");
            } else if (message.contains("not known") || message.contains("unreachable") || message.contains(host)) {
                // Network problems
                throw new StorageConnectException("Network problems.");
            } else {
                // Unknown problems...
                throw new StorageException(ex);
            }
            
        } catch (Exception ex) {
            throw new StorageException(ex);
        }
 
    }

    @Override
    public String getUser() {
        String user = "";
        try {
            connect();
            user = client.getStorageURL();
            if(user != null){
                user = user.substring(user.lastIndexOf("/")+1, user.length());
                if(user.endsWith("/")){
                    user = user.substring(0, user.length()-1);
                }
            }
        } catch (StorageConnectException ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
        
        return user;
    }

    public String getStorageIp() {
        String storageIp = "";
        try {
            connect();
            String authUrl = client.getAuthenticationURL();
            
            HttpGet get = new HttpGet(authUrl);            
            storageIp = get.getURI().getHost();
            
        } catch (StorageConnectException ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
        
        return storageIp;
    }

    @Override
    public void initStorage() throws StorageException {        
        String container = getConnection().getContainer();
        createContainer(container);
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile, CloneWorkspace workspace) 
            throws RemoteFileNotFoundException, StorageException {
        
        connect();
        File tempFile = null;
        InputStream is = null;
        
        try {
            String storageURL = workspace.getSwiftStorageURL();
            String container = workspace.getSwiftContainer();
            is = client.getSharedObjectAsStream(storageURL, container, remoteFile.getName());

            // Save to temp file
            tempFile = config.getCache().createTempFile(remoteFile.getName());
            FileUtil.writeFile(is, tempFile);

            FileUtil.copy(tempFile, localFile);

        } catch (Exception ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
            throw new StorageException("Unable to download file '" + remoteFile.getName(), ex);
        } finally {
            try {
                if (is != null){
                    is.close();
                }
            } catch (IOException ex) {
                logger.error("I/O Excdeption: ", ex);
                RemoteLogs.getInstance().sendLog(ex);
            } 
            
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile, CloneWorkspace workspace)
            throws LocalFileNotFoundException, StorageException, StorageQuotaExcedeedException {
        
        connect();

        // Check if exists
        /*Collection<RemoteFile> obj = list(remoteFile.getName()).values();

        if (obj != null && !obj.isEmpty()) {
            return;
        }*/
        
        try {
            // Upload
            //client.storeObjectAs(workspace.getSwiftContainer(), localFile, "application/x-Stacksync", remoteFile.getName());
            client.storeSharedObjectAs(workspace.getSwiftStorageURL(), workspace.getSwiftContainer(), localFile,
                    "application/x-Stacksync", remoteFile.getName());
        } catch (OverQuotaException ex) {
            logger.error("Quota limit exceeded. Could not upload file "+localFile.getName(), ex);
            throw new StorageQuotaExcedeedException(ex);
        } catch (Exception ex) {
            logger.error(ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list(String namePrefix, CloneWorkspace workspace) throws StorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}