/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.ast.cloudABE.kpabe.CipherText;
import com.stacksync.desktop.exceptions.ConfigException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author javigd
 */
public class AbeEncryption implements Encryption {

    //TODO: Default Access Structure for testing purposes. Must either be provided beforehand or computed.
    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    
    private String accessStructure;
    private CloudABEClient cabe;

    public AbeEncryption() throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter();
            this.accessStructure = DEFAULT_ACCESS_STRUCT;
            init();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }
    
    public AbeEncryption(String accessStructure) throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter();
            this.accessStructure = accessStructure;
            init();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    private void init() throws AttributeNotFoundException {
            cabe.setupABESystem(0, accessStructure);
    }

    @Override
    public synchronized AbeCipherData encrypt(PlainData data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        AbePlainData dataAbe = (AbePlainData) data;
        CipherText cipher = cabe.encryptData(dataAbe.getData(), dataAbe.getAttributes());
        return new AbeCipherData(cipher);
    }

    @Override
    public synchronized byte[] decrypt(CipherData data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        AbeCipherData cipher = (AbeCipherData) data;
        return cabe.decryptCipherText(cipher.toCipherText());
    }
    
//    private AbeCipherData getCipherData(CipherText cipherTxt) {
//        AbeCipherData cipher = new AbeCipherData();
//        ArrayList<ABEMetaComponent> components = new ArrayList<ABEMetaComponent>();
//        
//        cipher.setCipherText(cipherTxt.getEncrypted_message());
//        
//        for(String attribute : cipherTxt.getAttributes()) {
//            Attribute att = new Attribute();
//            att.setName(attribute);
//            String encryptedBytes = new String(cipherTxt.getEncrypted_attribute(attribute));
//            ABEMetaComponent component = new ABEMetaComponent(null, att, encryptedBytes, Long.MIN_VALUE);
//            components.add(component);
//        }
//        cipher.setComponents(components);
//        
//        return cipher;
//    }
//    
//    private CipherText getCipherText(AbeCipherData data) {
//        HashMap<String, byte[]> components = new HashMap<String, byte[]>();
//        ArrayList<String> attributes = new ArrayList<String>();
//        
//        for(ABEMetaComponent component : data.getComponents()) {
//            String attribute = component.getAttribute().getName();
//            byte[] bytes = component.getEncryptedPKComponent().getBytes();
//            components.put(attribute, bytes);
//            attributes.add(attribute);
//        }
//        
//        return new CipherText(attributes, data.getCipherText(), components);
//    }
}
