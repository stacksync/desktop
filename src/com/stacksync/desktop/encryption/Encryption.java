/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.stacksync.desktop.exceptions.ConfigException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author javigd
 */
public interface Encryption {
    
    public byte[] encrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException;

    public byte[] decrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException;
    
}
