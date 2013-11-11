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
package com.stacksync.desktop.connection.plugins.picasa;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.PhotoEntry;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.BitmapUtil;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 *
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class PicasaTransferManager extends AbstractTransferManager {

    private PicasawebService picasa;
    private URL albumURL;

    public PicasaTransferManager(PicasaConnection connection) {
        super(connection);

        picasa = new PicasawebService(Constants.APPLICATION_NAME);
    }

    @Override
    public PicasaConnection getConnection() {
        return (PicasaConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        if (albumURL != null) {
            return;
        }

        try {
            picasa.setUserCredentials(getConnection().getUsername(), getConnection().getPassword());
            albumURL = new URL("https://picasaweb.google.com/data/feed/api/user/default/albumid/" + getConnection().getAlbumId());
        } catch (Exception ex) {
            Logger.getLogger(PicasaTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageConnectException(ex.getMessage());
        }
    }

    @Override
    public void disconnect() {
        // Fressen.
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();

        File tempFile = null;
        File tempFileDec = null;

        try {
            // Find it 
            // TODO this is slllooooowwww

            PhotoEntry photo = find(remoteFile);

            if (photo == null) {
                throw new StorageException("Could not find file " + remoteFile.getName());
            }

            // The client API returns an URL to a resized JPG file. To get the
            // full size picture (BMP), we need to add the 'd' parameter.

            // TODO This is ugly, check out this site for details:
            // TODO http://groups.google.com/group/google-picasa-data-api/browse_thread/thread/6dcf693682f67d7b	    	    	    		
            String resizedURL = photo.getMediaContents().get(0).getUrl();

            int slashIndex = resizedURL.lastIndexOf("/");
            URL originalURL = new URL(resizedURL.substring(0, slashIndex)
                    + "/d" + resizedURL.substring(slashIndex));

            //System.out.println(originalURL);
            InputStream is = originalURL.openStream();

            // Download file
            tempFile = config.getCache().createTempFile();
            tempFileDec = config.getCache().createTempFile();

            FileUtil.writeFile(is, tempFile);
            BitmapUtil.decodeFromBitmap(tempFile, tempFileDec);

            // Move file
            if (!tempFileDec.renameTo(localFile)) {
                throw new StorageException("Rename to " + localFile.getAbsolutePath() + " failed.");
            }

        } catch (Exception ex) {
            
            Logger.getLogger(PicasaTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }

            if (tempFileDec != null && tempFileDec.exists()) {
                tempFileDec.delete();
            }
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();

        // Check if exists; skip upload if it does!
        if (find(remoteFile) != null) {
            return;
        }

        File tempBitmapFile = null;

        try {
            // Encode to bitmap
            tempBitmapFile = config.getCache().createTempFile();
            BitmapUtil.encodeToBitmap(localFile, tempBitmapFile);

            // Upload
            MediaFileSource bitmapSource = new MediaFileSource(tempBitmapFile, "image/bmp");

            PhotoEntry photo = new PhotoEntry();
            photo.setTitle(new PlainTextConstruct(remoteFile.getName()));
            photo.setMediaSource(bitmapSource);

            picasa.insert(albumURL, photo);
        }
        catch (Exception ex) {
            /* if (tempBitmapFile != null)
            tempBitmapFile.delete();*/

            System.err.println("error uploading: " + ex.getMessage() + " to");
            Logger.getLogger(PicasaTransferManager.class.getName()).log(Level.SEVERE, "error uploading file " + localFile + " to " + remoteFile + " (temp bitmap file: " + tempBitmapFile + ")", ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();

        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();

            AlbumFeed feed = picasa.getFeed(albumURL, AlbumFeed.class);

            for (PhotoEntry p : feed.getPhotoEntries()) {
                files.put(p.getTitle().getPlainText(), new RemoteFile(p.getTitle().getPlainText(), -1, p));
            }

            return files;
        } catch (Exception ex) {
            Logger.getLogger(PicasaTransferManager.class.getName()).log(Level.SEVERE, null, ex);
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
        connect();

        try {
            PhotoEntry photo = find(remoteFile);

            if (photo == null) {
                return;
            }

            photo.delete();
        } catch (Exception ex) {
            Logger.getLogger(PicasaTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    private PhotoEntry find(RemoteFile remoteFile) throws StorageException {
        for (RemoteFile rf : list().values()) {
            if (!rf.getName().equals(remoteFile.getName())) {
                continue;
            }

            return (PhotoEntry) rf.getSource();
        }

        return null;
    }


    @Override
    public void initStorage() throws StorageException {
        //nothing
    }

    @Override
    public String getUser() {
        return getConnection().getUsername();
    }

    public String getStorageIp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
