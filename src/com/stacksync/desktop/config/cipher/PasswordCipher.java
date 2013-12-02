package com.stacksync.desktop.config.cipher;

public interface PasswordCipher {
    
    public String encrypt(String plainPass);
    
    public String decrypt(String encryptedPass);
    
}
