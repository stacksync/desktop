package com.stacksync.desktop.config.cipher;

/*
 * This class is a simple example of how to use the password cipher for
 * the config files.
 * In this case the password is stored as plain text.
 */
public class DummyPasswordCipher implements PasswordCipher {

    @Override
    public String encrypt(String plainPass) {
        return plainPass;
    }

    @Override
    public String decrypt(String encryptedPass) {
        return encryptedPass;
    }
    
}
