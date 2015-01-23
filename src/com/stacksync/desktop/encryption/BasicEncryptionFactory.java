/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.stacksync.desktop.exceptions.ConfigException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author javigd
 */
public class BasicEncryptionFactory {

    public BasicEncryption getBasicEncryption(byte[] key) throws ConfigException {
        return new BasicEncryption(key);
    }
}
