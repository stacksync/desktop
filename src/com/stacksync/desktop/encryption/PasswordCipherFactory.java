package com.stacksync.desktop.encryption;

public class PasswordCipherFactory {
    
    public enum EncryptType {DUMMY, SIMPLE};
    
    public static PasswordCipher getPasswordEncrypter(EncryptType type) {
        
        PasswordCipher encrypter = null;
        
        switch(type) {
            case SIMPLE:
                encrypter = new SimplePasswordCipher();
                break;
            case DUMMY:
                encrypter = new DummyPasswordCipher();
        }
        
        return encrypter;
    }
    
}
