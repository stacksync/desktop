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
package com.stacksync.desktop.connection.plugins.ftp;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpTransferManager extends AbstractTransferManager { 
    private static final Logger logger = Logger.getLogger(FtpTransferManager.class.getSimpleName());
    private static final int CONNECT_RETRY_COUNT = 3;
    
    private static final int TIMEOUT_DEFAULT = 5000;
    private static final int TIMEOUT_CONNECT = 5000;
    private static final int TIMEOUT_DATA = 5000;
    //private static final int TIMEOUT_CONTROL_REPLY = 5000;
    
    private FTPClient ftp;

    public FtpTransferManager(FtpConnection connection) {
        super(connection);
        this.ftp = new FTPClient();
    } 
 
    @Override
    public FtpConnection getConnection() {
        return (FtpConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        for (int i=0; i<CONNECT_RETRY_COUNT; i++) {
            try {
                if (ftp.isConnected()) {
                    return;
                }

                logger.info("FTP client connecting to " + getConnection().getHost() + ":" + getConnection().getPort() + " ...");

                ftp.setConnectTimeout(TIMEOUT_CONNECT);
                ftp.setDataTimeout(TIMEOUT_DATA);	
                //ftp.setControlKeepAliveReplyTimeout(TIMEOUT_CONTROL_REPLY);
                ftp.setDefaultTimeout(TIMEOUT_DEFAULT);

                ftp.connect(getConnection().getHost(), getConnection().getPort());
                ftp.login(getConnection().getUsername(), getConnection().getPassword());
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!            
                
                return;
            } catch (Exception ex) {
                logger.error("FTP client connection failed. ", ex);               
                throw new StorageConnectException(ex);
            }                        
        }
        
        logger.error("RETRYING FAILED: FTP client connection failed. ");
    }

    @Override
    public void disconnect() {
        try {
            ftp.logout();
            ftp.disconnect();
        } catch (IOException ex) {
            logger.error("Failed desconnection.", ex);
        }
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();
        String remotePath = getConnection().getPath()+"/"+remoteFile.getName();
        File tempFile = null;
        
        try {
            // Download file
            tempFile = config.getCache().createTempFile();
            OutputStream tempFOS = new FileOutputStream(tempFile);
            
            logger.info("FTP: Downloading " + remotePath + " to temp file " + tempFile);
            ftp.retrieveFile(remotePath, tempFOS);

            tempFOS.close();

            // Move file
            logger.info("FTP: Renaming temp file " + tempFile + " to file " + localFile);
            
            FileUtil.copy(tempFile, localFile);
            /*if (!tempFile.renameTo(localFile)) {
                throw new StorageException("Rename to "+localFile.getAbsolutePath()+" failed.");
            }*/
        } catch (IOException ex) {            
            logger.error("Error while downloading file " + remoteFile.getName(), ex);
            throw new StorageException(ex);
        } finally {
            if(tempFile != null && tempFile.exists()){
                tempFile.delete();
            }
        }
        
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();
        
        // CCG
        String path = remoteFile.getName();
        if(path.startsWith("/")){
            path = path.substring(1);
        }
        
        String remotePath = getConnection().getPath();
        
        String[] folders = path.split("/");
        for(int i=0; i<folders.length-1; i++){
            try {
                
                remotePath += "/"+folders[i];
                
                String oldPath = ftp.printWorkingDirectory();
                
                boolean exist = ftp.changeWorkingDirectory(remotePath);
                if(!exist){
                    ftp.makeDirectory(remotePath);
                    //ftp.changeWorkingDirectory(folders[i]);
                } else {
                    if(oldPath != null) {
                        ftp.changeWorkingDirectory(oldPath);
                    }
                }
                
            } catch (IOException ex) {
                logger.error("FTP: Uploading failed ", ex);
                throw new StorageException(ex);
            }
        }
        
        // CCG
        remotePath +=  "/" + folders[folders.length-1];
        
        //String remotePath = getConnection().getPath()+"/"+path;
        folders[folders.length-1] = "temp-"+folders[folders.length-1];
        String tempRemotePath = getConnection().getPath();
        for(String folder : folders){
            tempRemotePath += "/"+folder;
        }
        
        //String remotePath = getConnection().getPath()+"/"+remoteFile.getName();
        //String tempRemotePath = getConnection().getPath()+"/temp-"+remoteFile.getName();

        try {
            // Upload to temp file
            InputStream fileFIS = new FileInputStream(localFile);
            logger.info("FTP: Uploading " + localFile + " to temp file " + tempRemotePath);          
            
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!
            
            if (!ftp.storeFile(tempRemotePath, fileFIS)) {
                throw new IOException("Error uploading file "+remoteFile.getName());
            }

            fileFIS.close();

            // Move
            logger.info("FTP: Renaming temp file " + tempRemotePath + " to file " + remotePath);
            
            ftp.rename(tempRemotePath, remotePath);
        } catch (IOException ex) {
            logger.error("Could not upload file " + localFile + " to " + remoteFile.getName(), ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();

        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();
            FTPFile[] ftpFiles = ftp.listFiles(getConnection().getPath());

            for (FTPFile f : ftpFiles) {
                files.put(f.getName(), new RemoteFile(f.getName(), f.getSize(), f));
                if (f.isDirectory()) {
                    files.putAll(getDirectoryList(f.getName()));
                }
            }

            return files;
        } catch (IOException ex) {
            logger.error("Unable to list FTP directory.", ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        connect();

        String path = getConnection().getPath() + "/" + remoteFile.getName();
        
        try {
            ftp.deleteFile(path);
        } catch (IOException ex) {
            logger.error("Could not delete file " + remoteFile.getName(), ex);
            throw new StorageException(ex);
        }
    }

    
    public Map<String, RemoteFile> getDirectoryList(String folderPath) throws StorageException{
        
        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();
            FTPFile[] ftpFiles = ftp.listFiles(getConnection().getPath() + "/" + folderPath);

            for (FTPFile f : ftpFiles) {
                files.put(folderPath+"/"+f.getName(), new RemoteFile(folderPath+"/"+f.getName(), f.getSize(), f));
                if (f.isDirectory()) {
                    files.putAll(getDirectoryList(folderPath+"/"+f.getName()));
                }
            }

            return files;
        } catch (IOException ex) {
            logger.error("Unable to list FTP directory.", ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void initStorage() throws StorageException {
        //nothing?        
    }

    @Override
    public String getUser() {
        return getConnection().getUsername();        
    }

    public String getStorageIp() {
        return getConnection().getHost();        
    }

}
