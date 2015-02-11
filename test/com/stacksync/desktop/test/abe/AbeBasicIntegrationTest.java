/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.abe;

import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.stacksync.desktop.encryption.AbeEncryption;
import com.stacksync.desktop.encryption.AbePlainData;
import com.stacksync.desktop.encryption.BasicCipherData;
import com.stacksync.desktop.encryption.BasicPlainData;
import com.stacksync.desktop.encryption.CipherData;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.encryption.PlainData;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.util.FileUtil;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author javigd
 */
public class AbeBasicIntegrationTest {

    private static final Logger logger = Logger.getLogger(AbeBasicIntegrationTest.class.getName());

    private static AbeEncryption encryption;

    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String ABE_RES_PATH = "./resources/conf/abe/";

    @BeforeClass
    public static void setUpClass() {
        System.out.println("[TestSetup] Setting up Attribute-Based encryption environment...");
        /* Set up the ABE encryption environment */
        try {
            encryption = new AbeEncryption(ABE_RES_PATH);
        } catch (Exception ex) {
            System.out.println("[TestSetup] Unable to load client data!");
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * ABE Encryption test: Simple encryption-decryption cycle. Single
     * attribute.
     */
    @Test
    public void dataEncryptionTest() {
        String message = "testing...";
        PlainData mymessage = null;
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("MarketingA");
        attSet.add("DesignA");
        attSet.add("DesignB");

        System.out.println("----------[dataEncryptionTest]---------");

        byte[] myKeyBytes = encryption.generateSymKey();

        // Plaindata object for key ABE encryption
        PlainData mykey = new AbePlainData(myKeyBytes, attSet);

        mymessage = new BasicPlainData(message.getBytes());

        try {
            System.out.println("[dataEncryptionTest] Original Message: " + message);
            // Encrypt key
            CipherData cipherKey = encryption.encrypt(mykey);
            System.out.println("[dataEncryptionTest] Original key: " + new String(myKeyBytes));
            // Encrypt message
//            byte[] myKeyBytes = key.getBytes("UTF-8");
            Encryption symEnc = encryption.getBasicEncryption(myKeyBytes);
            CipherData cipherMessage = symEnc.encrypt(mymessage);
            // Send chunks and metadata...
            // Decrypt key
            byte[] decryptedKey = encryption.decrypt(cipherKey);
            if (decryptedKey != null) {
                System.out.println("[dataEncryptionTest] Decrypted key: " + new String(decryptedKey));
            } else {
                System.out.println("[dataEncryptionTest] Unable to decrypt. ABE Decryption requierements unsatisfied");
            }
            // Get a basic encryption instance from decrypted key
            Encryption symEnc2 = encryption.getBasicEncryption(decryptedKey);
            // Decrypt message
            byte[] decryptedMessage = symEnc2.decrypt(cipherMessage);
            System.out.println("[dataEncryptionTest] Decrypted message: " + new String(decryptedMessage));

        } catch (Exception ex) {
            System.out.println("[dataEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

    /**
     * ABE Encryption test: Simple encryption-decryption cycle. Single
     * attribute.
     */
    @Test
    public void dataCompressedEncryptionTest() {
        String message = "testing...";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("MarketingA");
        attSet.add("DesignA");
        attSet.add("DesignB");

        System.out.println("----------[dataCompressedEncryptionTest]---------");

        byte[] myKeyBytes = encryption.generateSymKey();

        // Plaindata object for key ABE encryption
        PlainData mykey = new AbePlainData(myKeyBytes, attSet);

        try {
            System.out.println("[dataCompressedEncryptionTest] Original Message: " + message);
            // 1. Encrypt key using ABE
            System.out.println("[dataCompressedEncryptionTest] Original key: " + new String(myKeyBytes));
            System.out.println("[dataCompressedEncryptionTest] 1. Encrypting key using ABE encryption");
            CipherData cipherKey = encryption.encrypt(mykey);
            // 2. Compress message content
            System.out.println("[dataCompressedEncryptionTest] 2. Compressing data...");
//            byte[] compressedMessage = FileUtil.gzip(message.getBytes());
            // 3. Encrypt compressed data
            System.out.println("[dataCompressedEncryptionTest] 3. Encrypting compressed data (AES)...");
//            BasicPlainData mymessage = new BasicPlainData(compressedMessage);
            Encryption symEnc = encryption.getBasicEncryption(myKeyBytes);
            byte[] packed = FileUtil.pack(message.getBytes(), symEnc);
//            CipherData cipherMessage = symEnc.encrypt(mymessage);
            // (4) Send chunks and metadata...
            // (5) Get chunks and metadata back...
            System.out.println("[dataCompressedEncryptionTest] 4. Data & metadata ready to be sent to the cloud");
            System.out.println("[dataCompressedEncryptionTest] 5. Data & metadata downloaded from cloud");
            // Decrypt key
            // 6. Decrypt key using ABE
            System.out.println("[dataCompressedEncryptionTest] 6. Decrypting key using ABE encryption");
            byte[] decryptedKey = encryption.decrypt(cipherKey);
            if (decryptedKey != null) {
                System.out.println("[dataCompressedEncryptionTest] Decrypted key: " + new String(decryptedKey));
            } else {
                System.out.println("[dataCompressedEncryptionTest] Unable to decrypt. ABE Decryption requierements unsatisfied");
            }
            System.out.println("[dataCompressedEncryptionTest] 7. Decrypting data (AES)");
            // 7. Decrypt data
            Encryption symEnc2 = encryption.getBasicEncryption(decryptedKey);
//            byte[] decryptedData = symEnc2.decrypt(cipherMessage);
            // 8. unGzip data
            System.out.println("[dataCompressedEncryptionTest] 8. Uncompressing decrypted data...");
//            byte[] unGzippedData = FileUtil.gunzip(decryptedData);
            byte[] unpacked = FileUtil.unpack(packed, symEnc2);
            System.out.println("[dataCompressedEncryptionTest] Decrypted message: " + new String(unpacked));
        } catch (Exception ex) {
            System.out.println("[dataCompressedEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

}
