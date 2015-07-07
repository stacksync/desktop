package com.stacksync.desktop.encryption;

import com.ast.cloudABE.cloudABEClient.CloudInvitedABEClientAdapter;
import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.ast.cloudABE.kpabe.CipherText;
import com.ast.cloudABE.kpabe.KPABESecretKey;
import com.ast.cloudABE.kpabe.SystemKey;
import com.stacksync.desktop.exceptions.ConfigException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import org.apache.log4j.Logger;

/**
 *
 * @author ruizmarc
 */
public class AbeInvitedEncryption implements Encryption {

    private String accessStructure;
    private CloudInvitedABEClientAdapter cabe;
    private BasicEncryptionFactory bef;
    private final Logger logger = Logger.getLogger(AbeInvitedEncryption.class.getName());

    private SecureRandom random;

    public AbeInvitedEncryption(String abeResourcesPath) throws ConfigException {
        try {
            this.cabe = new CloudInvitedABEClientAdapter(abeResourcesPath);
            this.accessStructure = null;
            this.random = new SecureRandom();
            loadInit();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    public AbeInvitedEncryption(String abeResourcesPath, String accessStructure, SystemKey publicKey, KPABESecretKey secretKey) throws ConfigException {
        try {
            this.cabe = new CloudInvitedABEClientAdapter(abeResourcesPath);
            this.accessStructure = accessStructure;
            this.random = new SecureRandom();
            init(accessStructure, publicKey, secretKey);
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    private void init(String accessStructure, SystemKey publicKey, KPABESecretKey secretKey) throws AttributeNotFoundException {
        cabe.setupABESystem(0, accessStructure, publicKey, secretKey);
        this.bef = new BasicEncryptionFactory();
    }
    
    private void loadInit() throws AttributeNotFoundException {
        cabe.loadABESystem(0);
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
