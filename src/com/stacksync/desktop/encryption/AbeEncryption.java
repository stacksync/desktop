/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import com.stacksync.desktop.exceptions.ConfigException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author javigd
 */
public class AbeEncryption implements Encryption {

    //TODO: Move constant into the proper config file
    private static final String DEFAULT_XML_PATH = "./src/com/stacksync/desktop/encryption/attribute_universe.xml";
    //TODO: Default Access Structure for testing purposes. Must either be provided beforehand or computed.
    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    
    private String xmlPath;
    private String accessStructure;
    private CloudABEClient cabe;

    public AbeEncryption() throws ConfigException {
        try {
            cabe = new CloudABEClientAdapter();
            xmlPath = DEFAULT_XML_PATH;
            accessStructure = DEFAULT_ACCESS_STRUCT;
            init();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    private void init() {
            cabe.setupABESystem(0, cabe.getAttUniverseFromXML(xmlPath));
            cabe.newABEUser("stacksync_user", accessStructure);
    }

    @Override
    public byte[] encrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //TODO
        //cabe.encryptData(data, attributes);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] decrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //TODO
        //cabe.decryptData(data, attributes);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
