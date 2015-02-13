/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.abe;

import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.stacksync.desktop.encryption.AbeEncryption;
import com.stacksync.desktop.encryption.AbePlainData;
import com.stacksync.desktop.encryption.CipherData;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.encryption.PlainData;
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
public class AbeEnvironmentTest {

    private static final Logger logger = Logger.getLogger(AbeEnvironmentTest.class.getName());

    private static Encryption encAlice;
    private static Encryption encBob;

    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String ABE_RES_PATH = "./resources/conf/abe/";

    private static final String BOB_ACCESS_STRUCT = "(DesignA & DesignB)";

    @BeforeClass
    public static void setUpClass() {
        System.out.println("[TestSetup] Setting up Attribute-Based encryption environment...");
        /* Set up the ABE encryption environment */
        try {
            // Initialize ABE environment (Generate keys first)
            //Encryption generateEncryption = new AbeEncryption(DEFAULT_ACCESS_STRUCT, true);
            // Initialize testing ABE clients
            System.out.println("[TestSetup] Initializing ABE environment for Alice...");
            encAlice = new AbeEncryption(ABE_RES_PATH, DEFAULT_ACCESS_STRUCT);
            System.out.println("[TestSetup] Initializing ABE environment for Bob...");
            encBob = new AbeEncryption(ABE_RES_PATH, BOB_ACCESS_STRUCT);
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
     * ABE Encryption test: Simple encryption-decryption sharing.
     */
    @Test
    public void dataAbeSharingTest() {
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("DesignA");
        attSet.add("DesignB");

        PlainData data = new AbePlainData(message.getBytes(), attSet);

        try {
            CipherData cipher = encAlice.encrypt(data);
            System.out.println("[dataEncryptionTest] Original message: " + message + ". attribute set: " + attSet);
            byte[] output = encAlice.decrypt(cipher);
            if (output != null) {
                System.out.println("[dataEncryptionTest] Alice Decrypted: " + new String(output));
            } else {
                System.out.println("[dataEncryptionTest] Alice was unable to decrypt. ABE Decryption requierements unsatisfied");
            }
            byte[] output2 = encBob.decrypt(cipher);
            if (output != null) {
                System.out.println("[dataEncryptionTest] Bob Decrypted: " + new String(output2));
            } else {
                System.out.println("[dataEncryptionTest] Bob was unable to decrypt. ABE Decryption requierements unsatisfied");
            }
        } catch (InvalidKeyException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            System.out.println("[dataEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

    /**
     * ABE Encryption test: Simple encryption-decryption attribute set not
     * satisfied.
     */
    @Test
    public void badAbeSharinghTest() {
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("MarketingA");

        PlainData data = new AbePlainData(message.getBytes(), attSet);

        try {
            CipherData cipher = encAlice.encrypt(data);
            System.out.println("[badEncryptionTest] Original message: " + message + ". attribute set: " + attSet);
            byte[] output = encAlice.decrypt(cipher);
            if (output != null) {
                System.out.println("[badEncryptionTest] Alice Decrypted: " + new String(output));
            } else {
                System.out.println("[badEncryptionTest] Alice was unable to decrypt. ABE Decryption requierements unsatisfied");
            }
            byte[] output2 = encBob.decrypt(cipher);
            if (output2 != null) {
                System.out.println("[badEncryptionTest] Bob Decrypted: " + new String(output2));
            } else {
                System.out.println("[badEncryptionTest] Bob was unable to decrypt. ABE Decryption requierements unsatisfied");
            }
        } catch (InvalidKeyException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            System.out.println("[dataEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }
}
