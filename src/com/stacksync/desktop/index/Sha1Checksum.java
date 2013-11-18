package com.stacksync.desktop.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import javax.xml.bind.DatatypeConverter;
import org.apache.log4j.Logger;
import com.stacksync.desktop.logging.RemoteLogs;

/**
 * @author Guillermo Guerrero
 * @author Cristian Cotes
 */
public class Sha1Checksum implements ChecksumCreator {

    private static final Logger logger = Logger.getLogger(Sha1Checksum.class.getName());
    
    public Sha1Checksum() { }

    private static String toHexString(byte[] array) {
        return DatatypeConverter.printHexBinary(array);
    }

    private static byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    @Override
    public synchronized String createChecksum(byte[] data, int offset, int length) {
        byte[] dataSha1 = data.clone();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(dataSha1, offset, length);

            byte[] mdbytes = md.digest();
            String hash = toHexString(mdbytes);
            return hash;
        } catch (NoSuchAlgorithmException e) {
             //LogConfig.sendErrorLogs(logger, config.getMachineName() + "#", e);
            logger.error("No such algorithm: ", e);
        }

        return "-1";
    }

    //TODO This method uses adler32!!! This is not correct, it should be SHA1!!
    @Override
    public Long getFileChecksum(File file) {
        long checksum = -1;
        byte[] buffer = new byte[512];
        Checksum check = new Adler32();

        try {
            FileInputStream fis = new FileInputStream(file);
            CheckedInputStream cis = new CheckedInputStream(fis, check);
            
            int read = cis.read(buffer);
            while(read != -1){
                read = cis.read(buffer);
            }

            checksum = check.getValue();
        } catch (IOException ex) {
            logger.error("Error getting file checksum: "+ex);
        }
        return checksum;
    }
}