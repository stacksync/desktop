/*
 * Syncany, www.syncany.org
 * Copyright (C) 2012 Maurus Cuelenaere<mcuelenaere@gmail.com>
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
package com.stacksync.desktop.connection.plugins.dropbox;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.session.WebAuthSession;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.exceptions.LocalFileNotFoundException;
import com.stacksync.desktop.exceptions.RemoteFileNotFoundException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;

/**
 * @author Maurus Cuelenaere <mcuelenaere@gmail.com>
 */
public class DropboxTransferManager extends AbstractTransferManager {
    private final DropboxAPI<WebAuthSession> api;
    private String cachedListHash;
    private Map<String, RemoteFile> cachedListResult;

    public DropboxTransferManager(DropboxConnection connection) {
        super(connection);

        api = new DropboxAPI<WebAuthSession>(connection.getAuthSession());
    }

    @Override
    public void connect() throws StorageConnectException {
    }

    @Override
    public void disconnect() throws StorageException {
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws RemoteFileNotFoundException, StorageException {
        String name = "/" + remoteFile.getName();
        OutputStream out = null;
        try {
            out = new FileOutputStream(localFile);
            api.getFile(name, null, out, null);
        } catch (Exception ex) {
            if (ex instanceof DropboxServerException) {
                DropboxServerException sex = (DropboxServerException) ex;
                if (sex.error == 404) {
                    throw new RemoteFileNotFoundException(ex);
                }
            }

            throw new StorageException(ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    throw new StorageException(ex);
                }
            }
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws LocalFileNotFoundException, StorageException {
        if (!localFile.exists()) {
            throw new LocalFileNotFoundException();
        }

        String name = "/" + remoteFile.getName();
        InputStream in = null;
        try {
            in = new FileInputStream(localFile);
            api.putFile(name, in, localFile.length(), null, null);
        } catch (Exception ex) {
            throw new StorageException(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new StorageException(ex);
                }
            }
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws RemoteFileNotFoundException, StorageException {
        try {
            api.delete(remoteFile.getName());
        } catch (DropboxException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        try {
            Map<String, RemoteFile> result = new HashMap<String, RemoteFile>();
            DropboxAPI.Entry entry = api.metadata("", 0, cachedListHash, true, null);
            assert(!entry.isDir);

            for (DropboxAPI.Entry child : entry.contents) {
                if (child.isDeleted || child.isDir) {
                    continue;
                }

                String name = child.path.substring(1); // skip the first slash
                RemoteFile file = new RemoteFile(name, child.bytes);
                result.put(name, file);
            }

            // update cache
            cachedListHash = entry.hash;
            cachedListResult = result;

            return result;
        } catch (DropboxException ex) {
            if (ex instanceof DropboxServerException) {
                DropboxServerException sex = (DropboxServerException) ex;
                if (sex.error == 304) {
                    return cachedListResult;
                }
            }

            throw new StorageException(ex);
        }
    }

    @Override
    public void initStorage() throws StorageException {
        //nothing
    }

    @Override
    public String getUser() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getStorageIp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }    

}
