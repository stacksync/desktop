package com.stacksync.desktop.encryption;

public interface PasswordCipher {
    
    public String encrypt(String plainPass);
    
    public String decrypt(String encryptedPass);
    
}
