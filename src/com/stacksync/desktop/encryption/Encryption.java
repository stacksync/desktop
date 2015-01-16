/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author javigd
 */
public interface Encryption {
    
    public CipherData encrypt(PlainData data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, AttributeNotFoundException;

    public byte[] decrypt(CipherData data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException;
    
}
