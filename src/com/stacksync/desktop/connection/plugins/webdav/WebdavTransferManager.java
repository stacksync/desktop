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
package com.stacksync.desktop.connection.plugins.webdav;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;

/**
 *
 *
 * TODO cleanup method
 * TODO SSL support.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WebdavTransferManager extends AbstractTransferManager {     
    public WebdavTransferManager(WebdavConnection connection) {
        super(connection);
    } 
 
    @Override
    public WebdavConnection getConnection() {
        return (WebdavConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
    // Fressen
    }

    @Override
    public void disconnect() {
    // Fressen
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        String remoteURL = getConnection().getURL(remoteFile.getName());

        try {
            Sardine sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());

            // Download file
            File tempFile = config.getCache().createTempFile();
            OutputStream tempFOS = new FileOutputStream(tempFile);

            System.out.println("downloading "+remoteURL+" to temp file "+tempFile);
            InputStream in = sardine.get(remoteURL);

            byte[] buf = new byte[4096]; 

            int len;
            while ((len = in.read(buf)) > 0) {
                tempFOS.write(buf, 0, len);
            }	    

            tempFOS.close();
            in.close();

            // Move file
            if (!tempFile.renameTo(localFile)) {
                throw new StorageException("Rename to "+localFile.getAbsolutePath()+" failed.");
            }
        } catch (IOException ex) {
            Logger.getLogger(WebdavTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        String remoteURL = getConnection().getURL(remoteFile.getName());
        String tempRemoteURL = getConnection().getURL("temp-"+remoteFile.getName());

        try {
            Sardine sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());

            // Upload to temp file
            InputStream fileFIS = new FileInputStream(localFile);

            System.out.println("uploading "+localFile+" to temp file "+tempRemoteURL);	    	    
            sardine.put(tempRemoteURL, fileFIS);

            fileFIS.close();

            // Move
            sardine.move(tempRemoteURL, remoteURL);

        } catch (Exception ex) {
            System.err.println("error uploading: "+ex.getMessage());
            Logger.getLogger(WebdavTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {	
        try {
            Sardine sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());
            //List<DavResource> resources = sardine.getResources(getConnection().getRootURL());
            List<DavResource> resources = sardine.list(getConnection().getRootURL());

            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();
            for (DavResource res : resources) {
                files.put(res.getName(), new RemoteFile(res.getName(), -1, res));
            }

            return files;
        } catch (IOException ex) {
            Logger.getLogger(WebdavTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException("...");
        }
    }

   @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
        Map<String, RemoteFile> result = new HashMap<String, RemoteFile>();

        for (Map.Entry<String, RemoteFile> entry : list().entrySet()) {
            if (!entry.getKey().startsWith(namePrefix)) {
                continue;
            }

            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Override
    public void clean() throws StorageException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        try {
            Sardine sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());
            sardine.delete(getConnection().getURL(remoteFile.getName()));
        } catch (IOException ex) {
            Logger.getLogger(WebdavTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
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
