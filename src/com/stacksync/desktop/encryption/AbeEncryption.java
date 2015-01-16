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

    //TODO: Default Access Structure for testing purposes. This must be provided beforehand.
    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String abeResourcesPath = "./resources/abe/";

    private String accessStructure;
    private CloudABEClient cabe;

    public AbeEncryption() throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
            this.accessStructure = DEFAULT_ACCESS_STRUCT;
            init();
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    public AbeEncryption(String accessStructure) throws ConfigException {
        try {
            this.cabe = new CloudABEClientAdapter(abeResourcesPath);
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
    public synchronized AbeCipherData encrypt(PlainData data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, AttributeNotFoundException {
        AbePlainData dataAbe = (AbePlainData) data;
        CipherText cipher = cabe.encryptData(dataAbe.getData(), dataAbe.getAttributes());
        return new AbeCipherData(cipher);
    }

    @Override
    public synchronized byte[] decrypt(CipherData data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        AbeCipherData cipher = (AbeCipherData) data;
        return cabe.decryptCipherText(cipher.toCipherText());
    }

}
