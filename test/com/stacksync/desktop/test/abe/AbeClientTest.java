/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.abe;

import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.stacksync.desktop.encryption.AbeEncryption;
import com.stacksync.desktop.encryption.CipherData;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.encryption.AbePlainData;
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
import static org.junit.Assert.*;

/**
 *
 * @author javigd
 */
public class AbeClientTest {

    private static final Logger logger = Logger.getLogger(AbeClientTest.class.getName());

    private static Encryption encryption;

    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String ABE_RES_PATH = "./resources/conf/abe/";

    @BeforeClass
    public static void setUpClass() {
        System.out.println("[TestSetup] Setting up Attribute-Based encryption environment...");
        /* Set up the ABE encryption environment */
        try {
            encryption = new AbeEncryption(ABE_RES_PATH, DEFAULT_ACCESS_STRUCT);
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
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("MarketingA");

        PlainData data = new AbePlainData(message.getBytes(), attSet);

        try {
            CipherData cipher = encryption.encrypt(data);
            System.out.println("[dataEncryptionTest] Original message: " + message);
            byte[] output = encryption.decrypt(cipher);
            if (output != null) {
                System.out.println("[dataEncryptionTest] Decrypted Data: " + new String(output));
            } else {
                System.out.println("[dataEncryptionTest] Unable to decrypt. ABE Decryption requierements unsatisfied");
            }
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            System.out.println("[dataEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

    /**
     * ABE Encryption test: Policy Not satisfied.
     */
    @Test
    public void dataNotSatisfiedEncryptionTest() {
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("DesignB");

        PlainData data = new AbePlainData(message.getBytes(), attSet);

        try {
            CipherData cipher = encryption.encrypt(data);
            System.out.println("[dataNotSatisfiedEncryptionTest] Original message: " + message);
            byte[] output = encryption.decrypt(cipher);
            if (output != null) {
                System.out.println("[dataNotSatisfiedEncryptionTest] Decrypted Data: " + new String(output));
            } else {
                System.out.println("[dataNotSatisfiedEncryptionTest] Unable to decrypt. ABE Decryption requierements unsatisfied");
            }
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            System.out.println("[dataNotSatisfiedEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

    /**
     * ABE Encryption test: OR clause satisfied.
     */
    @Test
    public void dataComplexEncryptionTest() {
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("DesignA");
        attSet.add("DesignB");

        PlainData data = new AbePlainData(message.getBytes(), attSet);

        try {
            CipherData cipher = encryption.encrypt(data);
            System.out.println("[dataComplexEncryptionTest] Original message: " + message);
            byte[] output = encryption.decrypt(cipher);
            if (output != null) {
                System.out.println("[dataComplexEncryptionTest] Decrypted Data: " + new String(output));
            } else {
                System.out.println("[dataComplexEncryptionTest] Unable to decrypt. ABE Decryption requierements unsatisfied");
            }
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            System.out.println("[dataComplexEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

    /**
     * ABE Encryption: Resilience to Injection of inexistent attributes.
     */
    @Test
    public void dataInjectionEncryptionTest() {
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
        attSet.add("MarketingA");
        attSet.add("DesignA");
        attSet.add("DesignY");

        PlainData data = new AbePlainData(message.getBytes(), attSet);

        try {
            CipherData cipher = encryption.encrypt(data);
            System.out.println("[dataInjectionEncryptionTest] Original message: " + message);
            byte[] output = encryption.decrypt(cipher);
            if (output != null) {
                System.out.println("[dataInjectionEncryptionTest] Decrypted Data: " + new String(output));
            } else {
                System.out.println("[dataInjectionEncryptionTest] Unable to decrypt. ABE Decryption requierements unsatisfied");
            }
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            System.out.println("[dataInjectionEncryptionTest] Unable to decrypt. " + ex.getMessage());
        }
    }

}
