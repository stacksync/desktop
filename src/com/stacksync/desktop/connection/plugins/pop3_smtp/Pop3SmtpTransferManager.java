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
package com.stacksync.desktop.connection.plugins.pop3_smtp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.*;
import javax.mail.search.SubjectTerm;
import com.stacksync.desktop.connection.plugins.AbstractTransferManager;
import com.stacksync.desktop.connection.plugins.pop3_smtp.Pop3SmtpConnection.SmtpMode;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;

/**
 *
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @see http://www.vipan.com/htdocs/javamail.html
 * @see https://www.ibm.com/developerworks/forums/thread.jspa?messageID=14569010
 * @see http://www.oracle.com/technetwork/java/faq-135477.html
 */
public class Pop3SmtpTransferManager extends AbstractTransferManager {
    private static final Logger logger = Logger.getLogger(Pop3SmtpTransferManager.class.getPackage().getName());

    private Session smtpSession;
    private Transport smtpTransport;
    
    private Session pop3Session;

    private Store pop3Store;
    private Folder folder;

    public Pop3SmtpTransferManager(Pop3SmtpConnection account) {
        super(account);

        Properties props = new Properties();

        // SMTP
        // cp. http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
        props.put("mail.smtp.host", account.getSmtpHost());
        props.put("mail.smtp.port", Integer.toString(account.getSmtpPort()));

        if (account.getSmtpMode() == SmtpMode.TLS_STARTTLS) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            smtpSession = Session.getDefaultInstance(props, null);
        } else if (account.getSmtpMode() == SmtpMode.SSL) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", Integer.toString(account.getSmtpPort()));

            smtpSession = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getConnection().getSmtpUsername(), getConnection().getSmtpPassword());
            }
            });
        } else {
            smtpSession = Session.getDefaultInstance(props, null);
        }

        try {
            smtpTransport = smtpSession.getTransport(new URLName("smtp", account.getSmtpHost(), account.getSmtpPort(), "", account.getSmtpUsername(), account.getSmtpPassword()));
        } catch (NoSuchProviderException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }


        // POP3
        if (account.isPop3SslEnabled()) {
            props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.pop3.socketFactory.fallback", "false");
        }

        try {
            pop3Session = Session.getDefaultInstance(props, null);
            pop3Store = (Store) pop3Session.getStore(
            new URLName("pop3", account.getPop3Host(), account.getPop3Port(), "", account.getPop3Username(), account.getPop3Password()));
        } catch (NoSuchProviderException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Pop3SmtpConnection getConnection() {
        return (Pop3SmtpConnection) super.getConnection();
    }

    @Override
    public void connect() throws StorageConnectException {
        connectPop3();
        connectSmtp();
    }

    private void connectPop3() throws StorageConnectException {
        try {
            if (pop3Store.isConnected()) {
                return;
            }

            pop3Store.connect();
            folder = (Folder) pop3Store.getFolder("INBOX");
        } catch (MessagingException e) {
            throw new StorageConnectException(e);
        }
    }

    private void connectSmtp() throws StorageConnectException {
        try {
            if (smtpTransport.isConnected()) {
                return;
            }

            smtpTransport.connect(
                getConnection().getSmtpHost(),
                getConnection().getSmtpPort(),
                getConnection().getSmtpUsername(),
                getConnection().getSmtpPassword()
            );
        } catch (MessagingException e) {
            throw new StorageConnectException(e);
        }
    }

    @Override
    public void disconnect() throws StorageException {
        disconnectPop3();
        disconnectSmtp();
    }

    public void disconnectPop3() throws StorageException {
        try {
            if (folder.isOpen()) {
                folder.close(true);
            }

            if (pop3Store.isConnected()) {
                pop3Store.close();
            }
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    public void disconnectSmtp() throws StorageException {
        try {
            if (smtpTransport.isConnected()) {
                smtpTransport.close();
            }
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void download(RemoteFile remoteFile, File localFile) throws StorageException {
        connectPop3();

        try {
            // TODO Exception handling!
            MimeMessage msg = find(remoteFile);

            if (msg == null) {
                throw new StorageException("No file with name "+remoteFile.getName()+" found.");
            }

            folder.open(Folder.READ_ONLY);

            MimeMultipart content = (MimeMultipart) msg.getContent();
            MimeBodyPart chunk = (MimeBodyPart) content.getBodyPart("data");

            FileOutputStream fos = new FileOutputStream(localFile);
            InputStream is = (InputStream) chunk.getContent();

            int read;
            byte[] bytes = new byte[1024];

            while((read = is.read(bytes))!= -1) {
                fos.write(bytes, 0, read);
            }

            is.close();
            fos.close();

            folder.close(true);
        } catch (IOException ex) {
            throw new StorageException(ex);
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
        connectSmtp();

        try {
            // 1. CHECK IF EXISTS
            if (find(remoteFile) != null) {
                return;
            }

            // 2. CREATE MESSAGE
            MimeMessage msg = new MimeMessage(smtpSession);
            MimeMultipart multipart = new MimeMultipart();

            msg.setFrom(new InternetAddress(getConnection().getRcptAddress()));
            msg.setRecipients(Message.RecipientType.TO, new Address[] { new InternetAddress(getConnection().getRcptAddress()) });

            msg.setSubject(remoteFile.getName());
            //msg.setText("Clonebox chunk "+remoteFile.getName());
            msg.setFlag(Flag.SEEN, true);
            msg.setSentDate(new Date());

            // Chunk as attachment
            MimeBodyPart chunk = new MimeBodyPart();
            DataSource source = new FileDataSource(localFile.getAbsolutePath());
            chunk.setDataHandler(new DataHandler(source));
            chunk.setContentID("data");

            multipart.addBodyPart(chunk);

            // Set multipart message as content
            msg.setContent(multipart);

            // 3. UPLOAD MESSAGE
            smtpTransport.sendMessage(msg, new Address[] { new InternetAddress(getConnection().getRcptAddress()) });
        } catch (AddressException ex) {
            throw new StorageException(ex);
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public void delete(RemoteFile remoteFile) throws StorageException {
        connectPop3();

        try {
            MimeMessage msg = find(remoteFile);

            if (msg == null) {
                return;
            }

            folder.open(Folder.READ_WRITE);
            msg.setFlag(Flag.DELETED, true);
            folder.close(true);
        } catch (MessagingException ex) {
            throw new StorageException(ex);
        }	
    }

    @Override
    public Map<String, RemoteFile> list() throws StorageException {
        connectPop3();

        try {
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
        connectPop3();

        try {
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
        throw new UnsupportedOperationException("Not supported yet.");
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
        connectPop3();

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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getUser() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getStorageIp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
