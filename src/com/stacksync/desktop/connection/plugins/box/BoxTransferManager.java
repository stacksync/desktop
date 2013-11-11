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
package com.stacksync.desktop.connection.plugins.box;

import cn.com.believer.songyuanframework.openapi.storage.box.BoxExternalAPI;
import cn.com.believer.songyuanframework.openapi.storage.box.constant.BoxConstant;
import cn.com.believer.songyuanframework.openapi.storage.box.factories.BoxRequestFactory;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.*;
import cn.com.believer.songyuanframework.openapi.storage.box.impl.simple.SimpleBoxImpl;
import cn.com.believer.songyuanframework.openapi.storage.box.objects.BoxException;
import cn.com.believer.songyuanframework.openapi.storage.box.objects.UploadResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.binary.Base64;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 *
 * @see http://code.google.com/p/box4j/wiki/Box4J_Examples
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class BoxTransferManager extends AbstractTransferManager {    
    private BoxExternalAPI box;
    
    public BoxTransferManager(BoxConnection connection) {
        super(connection);
        box = new SimpleBoxImpl();
    } 
 
    @Override
    public BoxConnection getConnection() {
        return (BoxConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        try {
            // Get a ticket by API key.
            if (getConnection().getTicket() == null) {	
                GetTicketRequest getTicketRequest = BoxRequestFactory.createGetTicketRequest(getConnection().getApiKey());
                GetTicketResponse getTicketResponse = box.getTicket(getTicketRequest);

                getConnection().setTicket(getTicketResponse.getTicket());
                System.out.println("ticket : "+getTicketResponse.getTicket());
            }

            // Auth Token
            if (getConnection().getToken() == null) {	
                GetAuthTokenRequest getAuthTokenRequest = 
                    BoxRequestFactory.createGetAuthTokenRequest(getConnection().getApiKey(), getConnection().getTicket());

                GetAuthTokenResponse getAuthTokenResponse = box.getAuthToken(getAuthTokenRequest);
                getConnection().setLoginStatus(getAuthTokenResponse.getStatus());

                System.out.println("token : "+getAuthTokenResponse.getAuthToken());

                if (BoxConstant.STATUS_NOT_LOGGED_IN.equals(getAuthTokenResponse.getStatus())) {
                    throw new StorageConnectException("Could not log in to box.net. Token is not logged in. Log in at https://www.box.net/api/1.0/auth/"+getConnection().getTicket());
                }
                
                if (!BoxConstant.STATUS_GET_AUTH_TOKEN_OK.equals(getAuthTokenResponse.getStatus())) {                    
                    throw new StorageConnectException("Could not log in to box.net. Status = "+getAuthTokenResponse.getStatus());
                }
                
                getConnection().setToken(getAuthTokenResponse.getAuthToken());
            }	    
        }
        catch (Exception ex) {
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
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
        
        File tmpLocalFile;

        try {
            // Find file on storage
            RemoteFile foundRemoteFile = find(remoteFile);
            String remoteFileId = getFileId(foundRemoteFile);
            
            if (remoteFileId == null) {
                throw new StorageException("Could not find file "+remoteFile);
            }
            
            // Download it
            // Note: The "DownloadRequest" option cannot tell whether the transfer 
            //       was successful. That's why we do it like the API says.
            
            tmpLocalFile = config.getCache().createTempFile();
            
            URL fileURL = new URL("https://www.box.net/api/1.0/download/"+getConnection().getToken()+"/"+remoteFileId);
            InputStream is = fileURL.openStream();

            FileUtil.writeFile(is, tmpLocalFile);
            
            // Rename
            if (!tmpLocalFile.renameTo(localFile)) {
                throw new StorageException("Renaming downloaded file from "+tmpLocalFile+" to "+localFile+" failed.");
            }
        } 
        catch (IOException ex) {
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (BoxException ex) {
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();	

        try {
            // 0. Find; and skip if exists
            // TODO sloooow
            Object existingRemoteFile = find(remoteFile);

            if (existingRemoteFile != null) {
                return;
            }

            // 1. Upload
            Map<String, File> fileMap = new HashMap();
            fileMap.put(remoteFile.getName(), localFile);	    

            UploadRequest uploadRequest = 
                BoxRequestFactory.createUploadRequest(getConnection().getToken(), true, getConnection().getFolderId(), fileMap);

            UploadResponse uploadResponse = box.upload(uploadRequest);	    

            if (!"upload_ok".equals(uploadResponse.getStatus()) || uploadResponse.getUploadResultList().isEmpty()) {
                throw new StorageException("Unable to upload file "+remoteFile.getName()+". Status code: "+uploadResponse.getStatus());
            }

            // 2. Rename (if necessary)
            // The API doesn't allow to specify a chosen name, i.e. a rename might be necessary
            if (!localFile.getName().equals(remoteFile.getName())) {	    
                UploadResult uploadedFile = (UploadResult) uploadResponse.getUploadResultList().get(0);	    
                String boxFileId = uploadedFile.getFile().getFileId();

                RenameResponse renameResponse =
                    box.rename(BoxRequestFactory.createRenameRequest(getConnection().getApiKey(), getConnection().getToken(), "file", boxFileId, remoteFile.getName()));

                // Destination exists; that's okay: delete uploaded file!
                if ("e_rename_node".equals(renameResponse.getStatus())) {
                    box.delete(BoxRequestFactory.createDeleteRequest(getConnection().getApiKey(), getConnection().getToken(), "ile", boxFileId));		
                }

                // Other error
                else if (!"s_rename_node".equals(renameResponse.getStatus())) {
                    throw new StorageException("Unable to upload file "+remoteFile.getName()+". Rename failed. Status code: "+renameResponse.getStatus()); 
                }
            }
        }
        catch (Exception ex) {
            System.err.println("error uploading: "+ex.getMessage());
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();

        try {
            GetAccountTreeResponse accountTreeResponse = 
                box.getAccountTree(BoxRequestFactory.createGetAccountTreeRequest(getConnection().getApiKey(), getConnection().getToken(), getConnection().getFolderId(), new String[] { }));

            if (!"listing_ok".equals(accountTreeResponse.getStatus())) {
                throw new StorageException("List query failed. Status: "+accountTreeResponse.getStatus());	    
            }

            // Note:
            //   The result is a (1) base 64 encoded, (2) zipped (3) XML file
            //   See http://developers.box.net/w/page/12923929/ApiFunction_get_account_tree
            
            // (1) Decode Base 64
            String base64Xml = accountTreeResponse.getEncodedTree();
            InputStream decodedBase64 = new ByteArrayInputStream(Base64.decodeBase64(base64Xml));
            
            // (2) Unzip
            ZipInputStream zip = new ZipInputStream(decodedBase64);
            
            if (zip.getNextEntry() == null) {
                throw new StorageException("Cannot read folder tree. Invalid ZIP data.");
            }
            
            // (3) Read XML            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(zip);

            // Find files via XPath
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            Object result = xpath.evaluate("files/file", doc.getDocumentElement(), XPathConstants.NODESET);
            
            NodeList nodes = (NodeList) result;
            Map<String, RemoteFile> list = new HashMap<String, RemoteFile>();

            for (int i=0; i<nodes.getLength(); i++) {
                Node node = nodes.item(i);
                
                String name = node.getAttributes().getNamedItem("file_name").getTextContent();                
                Integer size = StringUtil.parseInt(node.getAttributes().getNamedItem("size").getTextContent(), -1);
                
                list.put(name, new RemoteFile(name, size, node));
            }
            
            return list;
        }
        catch (Exception ex) {
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
            throw new StorageException("...");
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        try {
            RemoteFile foundRemoteFile = find(remoteFile);
            String remoteFileId = getFileId(foundRemoteFile);
            
            if (remoteFileId == null) {
                return;
            }
            
            DeleteRequest request =
                    BoxRequestFactory.createDeleteRequest(getConnection().getApiKey(), 
                    getConnection().getToken(), "file", remoteFileId);
            
            DeleteResponse response = box.delete(request);
            
            if (!"s_delete_node".equals(response.getStatus())) {
                throw new StorageException("Unable to delete file "+remoteFile+" (file id "+remoteFileId+"): status = "+response.getStatus());
            }
        }
        catch (BoxException ex) {
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex) {
            Logger.getLogger(BoxTransferManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private RemoteFile find(RemoteFile remoteFile) throws BoxException, IOException, StorageException {
        return list().get(remoteFile.getName());
    }
    
    private String getFileId(RemoteFile remoteFile) {
        if (remoteFile == null ||remoteFile.getSource() == null 
                || !(remoteFile.getSource() instanceof Node)) {
            
            return null;
        }
        
        Node node = (Node) remoteFile.getSource();
        return node.getAttributes().getNamedItem("id").getTextContent();
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
