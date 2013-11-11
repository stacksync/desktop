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
package com.stacksync.desktop.connection.plugins.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.FileUtil;
/**
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Thomas Tschager <dontpanic@tschager.net>
 */
public class SftpTransferManager extends AbstractTransferManager {
    private static final Logger logger = Logger.getLogger(SftpTransferManager.class.getSimpleName());
    private static final int CONNECT_RETRY_COUNT = 3;
    
    private JSch jsch;
    private ChannelSftp sftp;
    private Session session;
    
    public SftpTransferManager(SftpConnection connection) {
        super(connection);
        this.jsch=new JSch();
    } 
 
    @Override
    public SftpConnection getConnection() {
        return (SftpConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        boolean isConnected = false;
        
        if(session != null){
            if(session.isConnected()){
                if(sftp != null){
                    if (sftp.isConnected() && session.isConnected()){                            
                        isConnected = true;
                    }else{
                        session.disconnect();
                    }
                } else{
                    session.disconnect();
                }
            }
        }
        
        if(!isConnected){
            for (int i=0; i<CONNECT_RETRY_COUNT; i++) {
                try {

                    logger.info(config.getMachineName() + "#SFTP client connecting to " + getConnection().getHost() + ":" + getConnection().getPort() + " ...");

                    if (getConnection().isKeyAuth()) {
                        jsch.addIdentity(getConnection().getKeyPath(), getConnection().getPassphrase());
                        this.session = jsch.getSession(getConnection().getUsername(), getConnection().getHost(), getConnection().getPort());
                        
                        Properties cf = new Properties();
                        cf.put("StrictHostKeyChecking", "no");
                        session.setConfig(cf);
                        session.connect();
                        if(!session.isConnected()){
                            logger.warn(config.getMachineName() + "#SFTP client: unable to connect (user/password) to " + getConnection().getHost() + ":" + getConnection().getPort() + " ...");
                        }

                    } else {
                        this.session = jsch.getSession(getConnection().getUsername(), getConnection().getHost(), getConnection().getPort());
                        
                        Properties cf = new Properties();
                        cf.put("StrictHostKeyChecking", "no");
                        session.setConfig(cf);
                        session.setPassword(getConnection().getPassword());
                        session.connect();
                        if(!session.isConnected()){
                            logger.warn(config.getMachineName() + "#SFTP client: unable to connect (user/password) to " + getConnection().getHost() + ":" + getConnection().getPort() + " ...");
                        }
                    }

                    this.sftp = (ChannelSftp) session.openChannel("sftp");
                    this.sftp.connect();
                    if(!sftp.isConnected()){
                        logger.warn(config.getMachineName() + "#SFTP client: unable to connect sftp Channel (" + getConnection().getHost() + ":" + getConnection().getPort() + ") ...");
                    }

                    return;
                } catch (Exception ex) {
                    logger.warn(config.getMachineName() + "#SFTP client connection failed.", ex);
                    throw new StorageConnectException(ex);
                }                        
            }

            logger.error(config.getMachineName() + "#RETRYING FAILED: SFTP client connection failed.");
        }
    }

    @Override
    public void disconnect() {
        if(sftp != null){
            this.sftp.quit();
            this.sftp.disconnect();
        }
        if(session != null){
            this.session.disconnect();
        }
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();        
        
        // Fixed files starting with /
        String remotePath = getConnection().getPath();
        if(remoteFile.getName().startsWith("/")){
            remotePath += remoteFile.getName();
        } else{
            remotePath += "/" + remoteFile.getName();
        }

        File tempFile = null;
        
        try {
            // Download file
            tempFile = config.getCache().createTempFile();
            OutputStream tempFOS = new FileOutputStream(tempFile);

            logger.info(config.getMachineName() + "#SFTP: Downloading " + remotePath + " to temp file " + tempFile);
            sftp.get(remotePath, tempFOS);

            tempFOS.close();

            // Move file
            logger.info(config.getMachineName() + "#SFTP: Renaming temp file " + tempFile + " to file " + localFile);
            
            FileUtil.copy(tempFile, localFile);
            /*if (!tempFile.renameTo(localFile)) {
                throw new StorageException("Rename to "+localFile.getAbsolutePath()+" failed.");
            }*/
        } catch (Exception ex) {

            logger.error(config.getMachineName() + "#Error while downloading file "+remoteFile.getName(), ex);
            throw new StorageException(ex);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }            
        }    
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {               
        connect();

        //String remotePath = getConnection().getPath()+remoteFile.getName();
        //String tempRemotePath = getConnection().getPath()+"temp-"+remoteFile.getName();
        // CCG
        String path = remoteFile.getName().replace("\\", "/");
        if(path.startsWith("/")){
            path = path.substring(1);
        }
        
        String remotePath = getConnection().getPath();
        try {
            sftp.cd(remotePath);       
            
            String[] folders = path.split("/");
            for(int i=0; i<folders.length-1; i++){
                try {
                    
                    List<LsEntry> dFiles = sftp.ls(remotePath);
                    boolean contains = false;
                    for(LsEntry entry: dFiles) {
                        String filename = entry.getFilename();
                        
                        if(filename.compareTo(folders[i]) == 0){
                            contains = true;
                            break;
                        }
                    }
   
                    if(!contains){
                        sftp.mkdir(folders[i]);
                    }
                    
                    remotePath += "/"+folders[i];
                    sftp.cd(remotePath);
                    
                } catch (SftpException ex) {
                    logger.error(config.getMachineName() + "#Exception: ", ex);
                }
            }
            
            // CCG
            remotePath +=  "/" + folders[folders.length-1];
            
            folders[folders.length-1] = "temp-"+folders[folders.length-1];
            String tempRemotePath = getConnection().getPath();
            for(String folder : folders){
                tempRemotePath += "/"+folder;
            }            
            
            try {
                // Upload to temp file
                InputStream fileFIS = new FileInputStream(localFile);

                
                logger.info(config.getMachineName() + "#SFTP: Uploading " + localFile + " to temp file " + tempRemotePath);
                sftp.put(fileFIS, tempRemotePath);

                fileFIS.close();

                // Move
                logger.info(config.getMachineName() + "#SFTP: Renaming temp file " + tempRemotePath + " to file " + remotePath);
                sftp.rename(tempRemotePath, remotePath);

            } catch (Exception ex) {                
                logger.error(config.getMachineName() + "#Could not upload file "+localFile+" to "+remoteFile.getName(), ex);
                throw new StorageException(ex);
            }
        } catch (SftpException ex) {
            logger.error(config.getMachineName() + "#Exception: ", ex);            
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();

        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();
            List<LsEntry> sftpFiles = sftp.ls(getConnection().getPath());

            for (LsEntry file: sftpFiles) {                 
                if(file.getFilename().compareTo(".") != 0 && file.getFilename().compareTo("..") != 0){
                    files.put(file.getFilename(), new RemoteFile(file.getFilename(), file.getAttrs().getSize(), file));
                    if(file.getLongname().startsWith("d")){
                        files.putAll(getDirectoryList(file.getFilename()));
                    }                    
                }
            }

            return files;
        }
        catch (SftpException ex) {
            logger.error(config.getMachineName() + "#Unable to list SFTP directory. --> " + ex.getMessage(), ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            sftp.rm(getConnection().getPath() + "/" + remoteFile.getName());
        } catch (Exception ex) {
            logger.error(config.getMachineName() + "#Could not delete file " + remoteFile.getName(), ex);
            throw new StorageException(ex);
        }
    }
    
    
    public Map<String, RemoteFile> getDirectoryList(String folderPath) throws StorageException{
        
        try {
            Map<String, RemoteFile> files = new HashMap<String, RemoteFile>();

            List<LsEntry> sftpFiles = sftp.ls(getConnection().getPath() + "/" + folderPath);
            for (LsEntry file: sftpFiles) {                
                if(file.getFilename().compareTo(".") != 0 && file.getFilename().compareTo("..") != 0){
                    files.put(folderPath+"/"+file.getFilename(), new RemoteFile(folderPath+"/"+file.getFilename(), file.getAttrs().getSize(), file));
                    if(file.getLongname().startsWith("d")){
                        files.putAll(getDirectoryList(folderPath+"/"+file.getFilename()));
                    }                    
                }
            }

            return files;
        } catch (SftpException ex) {
            logger.error(config.getMachineName() + "#Unable to list SFTP directory.", ex);
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
