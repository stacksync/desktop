package com.stacksync.desktop.encryption;

import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.ast.cloudABE.kpabe.CipherText;
import com.stacksync.desktop.exceptions.ConfigException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import org.apache.log4j.Logger;

/**
 *
 * @author javigd
 */
public class AbeEncryption implements Encryption {

    private String accessStructure;
    private CloudABEClient cabe;
    private BasicEncryptionFactory bef;
    private final Logger logger = Logger.getLogger(AbeEncryption.class.getName());

    private SecureRandom random;

    public AbeEncryption(String abeResourcesPath) throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
            this.accessStructure = null;
            this.random = new SecureRandom();
            init();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    public AbeEncryption(String abeResourcesPath, String accessStructure) throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
            this.accessStructure = accessStructure;
            this.random = new SecureRandom();
            init();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    private void init() throws AttributeNotFoundException {
        logger.info("[ABE Benchmarking - ABE Setup] Setting access structure to: " + accessStructure);
        cabe.setupABESystem(0, accessStructure);
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

    public BasicEncryption getBasicEncryption(byte[] key) throws ConfigException {
        return bef.getBasicEncryption(key);
    }

    public byte[] generateSymKey() {
        String str = new BigInteger(130, random).toString(32);
        return str.getBytes();
    }
}
