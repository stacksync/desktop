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

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author oubou68, pheckel
 */
public abstract class RestTransferManager extends AbstractTransferManager {
    private static final Logger logger = Logger.getLogger(RestTransferManager.class.getSimpleName());
    private static final int CACHE_LIST_TIME = 60000;
    
    private RestStorageService service;
    private StorageBucket bucket;
    
    private Long cachedListUpdated;
    /**
     * Used for the upload function to determine whether a file is already uploaded
     */
    private Map<String, RemoteFile> cachedList;

    public RestTransferManager(RestConnection connection) {
        super(connection);
    }

    @Override
    public RestConnection getConnection() {
        return (RestConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        try {
            if (service == null) {
                service = createService();
            }

            if (bucket == null) {
                bucket = createBucket();
            }
        } catch (ServiceException ex) {
            throw new StorageConnectException("Unable to connect to S3: " + ex.getMessage(), ex);
        }
    }

    protected abstract RestStorageService createService() throws ServiceException;

    protected abstract StorageBucket createBucket();

    @Override
    public void disconnect() throws StorageException {
        // Fressen.
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();

        File tempFile = null;

        try {
            // Download
            StorageObject obj = service.getObject(bucket.getName(), remoteFile.getName());

            tempFile = config.getCache().createTempFile(remoteFile.getName());
            FileUtil.writeFile(obj.getDataInputStream(), tempFile);

            // SNM 6/01/11 Windows doesn't allow renaming on top of a file
            if(localFile.exists()){
                localFile.delete();
            }

            tempFile.renameTo(localFile);
        } catch (Exception ex) { 
            throw new StorageException("Unable to download file '" + remoteFile.getName(), ex);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
            
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            // Skip if file exists
            Map<String, RemoteFile> list = getList(false);
            
            if (list.containsKey(remoteFile.getName())) {
                return;
            }

            // Read file entirely
            byte[] fileBytes = FileUtil.readFileToByteArray(localFile); // WARNING! Read ENTIRE file!

            StorageObject fileObject = new StorageObject(remoteFile.getName(), fileBytes);
            service.putObject(bucket.getName(), fileObject);
            
            // Add to cache
            cachedList.put(remoteFile.getName(), remoteFile);

        } catch (Exception ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            service.deleteObject(bucket.getName(), remoteFile.getName());
        }
        catch (ServiceException ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        return list(null);
    }

    @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
        connect();

        try {
            Map<String, RemoteFile> completeList = getList(true);
            
            if (namePrefix == null) {
                return completeList;
            }
            
            // Filtered (prefix given)
            Map<String, RemoteFile> filteredList = new HashMap<String, RemoteFile>();

            for (Map.Entry<String, RemoteFile> e : completeList.entrySet()) {
                if (e.getKey().startsWith(namePrefix)) {
                    filteredList.put(e.getKey(), e.getValue());
                }
            }
            
            return filteredList;

        }
        catch (ServiceException ex) {
            Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void clean() throws StorageException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private synchronized Map<String, RemoteFile> getList(boolean forceRefresh) throws ServiceException {
        // Used cached list
        if (!forceRefresh && cachedListUpdated != null && cachedListUpdated+CACHE_LIST_TIME > System.currentTimeMillis()) {
            logger.log(Level.FINEST, "getList(): using cached list from {0}", new Date(cachedListUpdated));            
            return cachedList;
        }
        
        // Refresh cache
        Map<String, RemoteFile> list = new HashMap<String, RemoteFile>();
        String bucketName = bucket.getName();
        StorageObject[] objects = service.listObjects(bucketName);

        for (StorageObject obj : objects) {
            list.put(obj.getName(), new RemoteFile(obj.getName(), -1, obj));
        }
        
        cachedList = list;
        cachedListUpdated = System.currentTimeMillis();        
        
        return list;
    }
    
    @Override
    public void initStorage() throws StorageException {
        //nothing
    }

    @Override
    public String getUser() {
        return getConnection().getUsername();
    }    
}
