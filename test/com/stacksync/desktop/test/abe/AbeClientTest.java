/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.abe;

import com.ast.cloudABE.kpabe.CipherText;
import com.stacksync.desktop.encryption.AbeEncryption;
import com.stacksync.desktop.encryption.CipherData;
import com.stacksync.desktop.encryption.AbeCipherData;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.encryption.AbePlainData;
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
    
    private static final String[] defaultAttSet = {"MarketingA", "DesignA",  "DesignB"};
    
//    private static void doNewUser() {
//        String username = usernames[users.size()];
//        
//        /* Set the new user access policy */
//        String attSet = UIUtils.getAccessStructure(defaultAttSets[0]);
//        System.out.println("[NewUser] Setting up access logical expression to: " + attSet);
//        
//        /* Create and save a new instance of KPABEClient for testing purposes (download) */
//        KPABEClient newUserClient = new KPABEClient();
//        users.put(username, newUserClient);
//        
//        /* Generate credentials for the new user and send them to the Cloud */
//        System.out.println("[NewUser] Processing New User...");
//        abeclient.newABEUser(username, attSet);
//        System.out.println("[NewUser] New User operation done!");
//    }
//    
//    private static void doUpload(long id) {
//
//        // Encrypt a file with the chosen attributes
//        String this_filename = "testfile" + id + ".data";
//        String this_filepath = filepath + this_filename;
//        String ori_message = "[NewFile] This is the content of the Test File Number " + id;
//        ArrayList<String> data_attributes = new ArrayList<String>();
//        data_attributes.addAll(UIUtils.getAttributes(abeclient.getAttribute_universe()));
//
//        try {
//            System.out.println("[NewFile] Creating new file to be uploaded... id:" + id);
//            Files.write(Paths.get(this_filepath), ori_message.getBytes());
//        } catch (IOException ex) {
//            logger.log(Level.SEVERE, null, ex);
//            System.out.println("[NewFile] ERROR: Unable to write file test data");
//        }
//
//        // Generate symmetric key for files
//        SecretKey symmetricKey = abeclient.generateAESKey();
//        
//        /* Generate New ABE File metadata and send it to the Cloud */
//        System.out.println("[NewFile] Processing New File...");
//        abeclient.newABEFile(id, symmetricKey, data_attributes);
//        System.out.println("[NewFile] New File operation done!");
//        
//    }
//    
//    //TODO
//    private static void doDownload(long nfiles) throws IOException {
//        int fileID;
//        String userID;
//
//        try {
//            fileID = UIUtils.getFileID(nfiles);
//            userID = UIUtils.getUser(users.keySet());
//        } catch (NullPointerException | InputMismatchException e) {
//            fileID = 0;
//            userID = usernames[0];
//            System.out.println("ERR: Setting fileID by default to: " + fileID + " and userID to: " + userID);
//        }
//
//        String filename = "testfile" + fileID + ".data";
//        KPABEClient dClient = users.get(userID);
//        KPABESecretKey usrSK = abeclient.getUserMap().get(userID).getSecretKey();
//        dClient.downloadFile(filename, userID, "./data/downloaded/" + filename, usrSK);
//
//        System.out.println("READING CONTENT...");
//        System.out.println(new String(Files.readAllBytes(Paths.get("./data/downloaded/" + filename))));
//    }
//
//    //TODO
//    private static void doRevoke() {
//        String user = UIUtils.getUser(users.keySet());
//        KPABESecretKey usrSK = abeclient.getUserMap().get(user).getSecretKey();
//        abeclient.revokeUser(user, CABEConstants.MK_PATH, CABEConstants.PK_PATH, CABEConstants.GG_PATH, usrSK);
//    }

    @BeforeClass
    public static void setUpClass() {
        
        /* Set up the ABE encryption environment */
        try {
            encryption = new AbeEncryption();
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
    
    @Test
    public void dataEncryptionTest() {
        String message = "Test";
        ArrayList<String> attSet = new ArrayList<String>();
//        attSet.add("MarketingA");
        attSet.add("DesignA");
        attSet.add("DesignB");

        AbePlainData data = new AbePlainData(message.getBytes(), attSet);
        
        try {
            CipherData cipher = encryption.encrypt(data);
            System.out.println("[dataEncryptionTest] Original message: " + message);
            byte[] output = encryption.decrypt(cipher);
            System.out.println("[dataEncryptionTest] Decrypted Data: " + new String(output));
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(AbeClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
