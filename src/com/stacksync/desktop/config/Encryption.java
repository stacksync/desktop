/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.config;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.exceptions.ConfigException;

public class Encryption implements Configurable {
    private String password;
    private String cipherStr;
    private Integer keylength;

    private byte[] key;
    private SecretKeySpec keySpec;
    private Cipher cipher;

    public Encryption() {
        // All set by init()
        password = null;
        cipherStr = null;
        keylength = null;

        key = null;
        keySpec = null;
        cipher = null;
    }

    /**
     * bit must be dividable by 8.
     *
     * @param password
     * @param bit
     */
    public void init() throws ConfigException {
        if(cipherStr.toLowerCase().compareTo("none") != 0){

            try {
                // Create key by hashing the password
                this.key = new byte[keylength/8];

                MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
                msgDigest.reset();

                byte[] longkey = msgDigest.digest(password.getBytes("UTF-8"));
                if (longkey.length == key.length) {
                    this.key = longkey;
                } else if (longkey.length > key.length) {
                    System.arraycopy(longkey, 0, key, 0, key.length);
                } else if (longkey.length < key.length) {
                    throw new RuntimeException("Invalid key length '"+keylength+"' bit; max 256 bit supported.");
                }

                // Create the cipher object with the generated key
                this.keySpec = new SecretKeySpec(key, cipherStr); // AES -> 128/192 bit
                this.cipher = Cipher.getInstance(cipherStr);

                // Check
                byte[] testBytes = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

                if (!Arrays.equals(decrypt(encrypt(testBytes)), testBytes)) {
                    throw new ConfigException("Test encrypt/decrypt cycle failed.");
                }
            } catch (Exception e) {
                throw new ConfigException(e.getMessage() + "\n AES(128) DES(64) 3DES(192)");
            }
        }
    }

    public String getCipherStr() {
        return cipherStr;
    }

    public void setCipherStr(String cipherStr) {         
        this.cipherStr = cipherStr;
        if(this.cipherStr.toLowerCase().compareTo("3des") == 0){
            this.cipherStr = "DESede";
        }
    }

    public Integer getKeylength() {
        return keylength;
    }

    public void setKeylength(Integer keylength) {
        this.keylength = keylength;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public synchronized byte[] encrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if(cipherStr.toLowerCase().compareTo("none") != 0){
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);            
        } else {
            return data;
        }        
    }

    public synchronized byte[] decrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if(cipherStr.toLowerCase().compareTo("none") != 0){            
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(data);        
        } else {
            return data;
        }        
    }
    
    @Override
    public void load(ConfigNode node) throws ConfigException {
        password = node.getProperty("password");
        cipherStr = node.getProperty("cipher");
        keylength = node.getInteger("keylength");

        if (password == null) {
            throw new ConfigException("Password must be specified in encryption.");
        }

        if (cipherStr == null) {
            cipherStr = Constants.DEFAULT_ENCRYPTION_CIPHER;
        }

        if (keylength == null) {
            keylength = Constants.DEFAULT_ENCRYPTION_KEYLENGTH;
        }

        init();
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("password", password);
        node.setProperty("cipher", cipherStr);
        node.setProperty("keylength", keylength);
    }
    
    @Override
    public String toString(){
        if(cipherStr.toLowerCase().compareTo("none") == 0){
            return cipherStr;
        } else {
            return cipherStr + ", " + keylength + " bits";
        }
    }
}
