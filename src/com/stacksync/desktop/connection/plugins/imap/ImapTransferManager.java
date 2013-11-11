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
package com.stacksync.desktop.connection.plugins.imap;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SubjectTerm;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.connection.plugins.imap.ImapConnection.Security;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * TODO imap cleanup method
 * TODO handle duplicate chunks in repo
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @see http://www.vipan.com/htdocs/javamail.html
 * @see https://www.ibm.com/developerworks/forums/thread.jspa?messageID=14569010
 * @see http://www.oracle.com/technetwork/java/faq-135477.html
 */
public class ImapTransferManager extends AbstractTransferManager {
    private static final Logger logger = Logger.getLogger(ImapTransferManager.class.getPackage().getName());

    private Session session;
    private IMAPStore store;
    private IMAPFolder folder;

    public ImapTransferManager(ImapConnection connection) {
        super(connection);

        String protocol = (connection.getSecurity() == Security.SSL) ? "imaps" : "imap";
        URLName url = new URLName(protocol, connection.getHost(), connection.getPort(), "", connection.getUsername(), connection.getPassword());

        Properties props = new Properties();

        props.setProperty("mail.store.protocol", protocol);
        props.setProperty("mail.imap.connectiontimeout", "5000");
        props.setProperty("mail.imap.timeout", "5000");

        // SSL
        if (connection.getSecurity() == Security.SSL) {
            props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.imap.socketFactory.fallback", "false");
        }
        
        // STARTTLS
        else if (connection.getSecurity() == Security.STARTTLS) {
            props.setProperty("mail.imap.starttls.enable", "true");
            props.setProperty("mail.imap.starttls.required", "true");
        }

        try {
            session = Session.getDefaultInstance(props, null);
            store = (IMAPStore) session.getStore(url);
        }
        catch (NoSuchProviderException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ImapConnection getConnection() {
        return (ImapConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        try {	    
            if (store.isConnected()) {
                return;
            }

            logger.log(Level.INFO, "IMAP: Connecting to {0}, folder {1}", new Object[]{getConnection().getHost(), getConnection().getFolder()});
            
            store.connect();
            folder = (IMAPFolder) store.getFolder(getConnection().getFolder());
        } catch (MessagingException e) {
            throw new StorageConnectException(e);
        }
    }

    @Override
    public void disconnect() throws StorageException {
        try {
            logger.log(Level.INFO, "IMAP: Disconnecting from {0}, folder {1}", new Object[]{getConnection().getHost(), getConnection().getFolder()});
            
            if (folder.isOpen()) {
                folder.close(true);
            }

            if (store.isConnected()) {
                store.close();
            }
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connect();

        try {
            // TODO Exception handling!
            MimeMessage msg = find(remoteFile);

            if (msg == null) {
                throw new StorageException("No file with name "+remoteFile.getName()+" found.");
            }

            logger.log(Level.INFO, "IMAP: Downloading {0} to {1}", new Object[]{remoteFile.getName(), localFile});
            
            // Get attachment input stream
            folder.open(Folder.READ_ONLY);

            MimeMultipart content = (MimeMultipart) msg.getContent();
            MimeBodyPart chunk = (MimeBodyPart) content.getBodyPart("data");

            InputStream is = (InputStream) chunk.getContent();

            // Write to temp file
            File tempLocalFile = config.getCache().createTempFile(localFile.getName());
            //FileOutputStream tempFOS = new FileOutputStream(tempLocalFile);
            
            FileUtil.writeFile(is, tempLocalFile);
            
            // Rename to final file
            if (!tempLocalFile.renameTo(localFile)) {
                throw new StorageException("Unable to rename downloaded file "+tempLocalFile+" to "+localFile);
            }
            
            // Close!
            folder.close(true);
        } catch (Exception ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            // 1. CHECK IF EXISTS
            if (find(remoteFile) != null) {
                logger.log(Level.INFO, "IMAP: No need to upload {0}. Already exists.", new Object[]{remoteFile.getName()});                
                return;
            }

            logger.log(Level.INFO, "IMAP: Uploading {0} to {1} ...", new Object[]{localFile, remoteFile.getName()});
            
            // 2. CREATE MESSAGE
            MimeMessage msg = new MimeMessage(session);
            MimeMultipart multipart = new MimeMultipart();

            msg.setSubject(remoteFile.getName());
            //msg.setText("Clonebox chunk "+remoteFile.getName());
            msg.setFlag(Flag.SEEN, true);

            // Chunk as attachment
            MimeBodyPart chunk = new MimeBodyPart();
            DataSource source = new FileDataSource(localFile.getAbsolutePath());
            chunk.setDataHandler(new DataHandler(source));
            chunk.setContentID("data");

            multipart.addBodyPart(chunk);

            // Set multipart message as content
            msg.setContent(multipart);

            // 3. UPLOAD MESSAGE
            folder.appendMessages(new Message[] { msg });
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        connect();

        try {
            MimeMessage msg = find(remoteFile);

            if (msg == null) {
                logger.log(Level.INFO, "IMAP: No need to delete {0}. Does not exist.", new Object[]{remoteFile.getName()});
                return;
            }
        
            logger.log(Level.INFO, "IMAP: Deleting {0} ...", new Object[]{remoteFile.getName()});
        
            folder.open(Folder.READ_WRITE);
            msg.setFlag(Flag.DELETED, true);
            folder.close(true);
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connect();

        try {
            logger.log(Level.INFO, "IMAP: Retrieving file list ...");
            
            Map<String, RemoteFile> list = new HashMap<String, RemoteFile>();

            folder.open(Folder.READ_ONLY);
            Message[] msgs = folder.getMessages();

            for (Message msg : msgs) {
                list.put(msg.getSubject(), new RemoteFile(msg.getSubject(), -1, msg));
            }

            folder.close(true);
            return list;
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public Map<String, RemoteFile> list(String namePrefix) throws StorageException {
        connect();

        try {
            logger.log(Level.INFO, "IMAP: Retrieving file list with prefix {0} ...", new Object[]{namePrefix});
            
            Map<String, RemoteFile> list = new HashMap<String, RemoteFile>();

            folder.open(Folder.READ_ONLY);
            Message[] msgs = folder.search(new SubjectTerm(namePrefix));

            for (Message msg : msgs) {
                // Double check, since search() does not look for prefix, but for occurance
                if (!msg.getSubject().startsWith(namePrefix)) {
                    continue;
                }

                list.put(msg.getSubject(), new RemoteFile(msg.getSubject(), -1, msg));
            }

            folder.close(true);
            return list;
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void clean() throws StorageException {
        // TODO clean duplicate filenames cp find()-method
        super.clean();
    }

    /**
     * Finds the correct message by the filename of the remote file.
     *
     * @param remoteFile
     * @return
     * @throws MessagingException
     * @throws StorageException
     */
    private MimeMessage find(RemoteFile remoteFile) throws MessagingException, StorageException {
        folder.open(Folder.READ_ONLY);
        Message[] msgs = folder.search(new SubjectTerm(remoteFile.getName()));
        folder.close(true);

        if (msgs.length == 0) {
            return null;
        }

        if (msgs.length > 1) {
            logger.log(Level.WARNING, "Inconsistent storage: {0} chunks with name {1} found; taking the first one!", new Object[]{msgs.length, remoteFile.getName()});
        }

        return (MimeMessage) msgs[0];
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
        return getConnection().getHost();
    }

}
