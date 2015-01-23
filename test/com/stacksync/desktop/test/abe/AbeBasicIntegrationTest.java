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

    @BeforeClass
    public static void setUpClass() {
        System.out.println("[TestSetup] Setting up Attribute-Based encryption environment...");
        /* Set up the ABE encryption environment */
        try {
            encryption = new AbeEncryption(DEFAULT_ACCESS_STRUCT);
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
     * ABE Encryption test: Simple encryption-decryption. Single attribute.
     */
    @Test
    public void dataEncryptionTest() {
        String key = "mykey";
        String message = "test message";
        PlainData mymessage = null;
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("MarketingA");
        attSet.add("DesignA");
        attSet.add("DesignB");

        // Plaindata object for key ABE encryption
        PlainData mykey = new AbePlainData(key.getBytes(), attSet);

        //Plaindata object for message symmetric encryption
        try {
            mymessage = new BasicPlainData(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AbeBasicIntegrationTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            System.out.println("[dataEncryptionTest] Original Message: " + message);
            // Encrypt key
            CipherData cipherKey = encryption.encrypt(mykey);
            System.out.println("[dataEncryptionTest] Original key: " + key);
            // Encrypt message
//            byte[] myKeyBytes = key.getBytes("UTF-8");
            byte[] myKeyBytes = encryption.generateSymKey();
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
}
