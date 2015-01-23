package com.stacksync.desktop.encryption;

import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.ast.cloudABE.kpabe.CipherText;
import com.stacksync.desktop.exceptions.ConfigException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;

/**
 *
 * @author javigd
 */
public class AbeEncryption implements Encryption {

    //TODO: Default Access Structure for testing purposes. This must be provided beforehand.
    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String abeResourcesPath = "./resources/abe/";

    private String accessStructure;
    private CloudABEClient cabe;
    private BasicEncryptionFactory bef;
    
    private SecureRandom random;

    public AbeEncryption() throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
            this.accessStructure = DEFAULT_ACCESS_STRUCT;
            this.random = new SecureRandom();
            init(false);
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    public AbeEncryption(String accessStructure) throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
            this.accessStructure = accessStructure;
            this.random = new SecureRandom();
            init(false);
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }
    
    public AbeEncryption(String accessStructure, boolean generate) throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
            this.accessStructure = accessStructure;
            this.random = new SecureRandom();
            init(generate);
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    private void init(boolean generate) throws AttributeNotFoundException {
        cabe.setupABESystem(0, accessStructure, generate);
        this.bef = new BasicEncryptionFactory();
    }

    @Override
    public synchronized AbeCipherData encrypt(PlainData data) throws InvalidKeyException, AttributeNotFoundException {
        AbePlainData dataAbe = (AbePlainData) data;
        CipherText cipher = cabe.encryptData(dataAbe.getData(), dataAbe.getAttributes());
        return new AbeCipherData(cipher);
    }

    @Override
    public synchronized byte[] decrypt(CipherData data) {
        AbeCipherData cipher = (AbeCipherData) data;
        return cabe.decryptCipherText(cipher.toCipherText());
    }
    
    public BasicEncryption getBasicEncryption(String password) throws ConfigException {
        return bef.getBasicEncryption(password);
    }

    public String generateSymKey() {
        return new BigInteger(130, random).toString(32);
    }
}
