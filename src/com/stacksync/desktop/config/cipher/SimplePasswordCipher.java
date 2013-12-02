package com.stacksync.desktop.config.cipher;

/*
 * This class is a simple example of how to use the password cipher for
 * the config files.
 */
public class SimplePasswordCipher implements PasswordCipher {

    /*
     * This function will encrypt the password by incrementing each character.
     */
    @Override
    public String encrypt(String plainPass) {
        
        int total = plainPass.length();
        char[] encrypted = new char[total];
        for (int i = 0; i < total; i++) {
            int value = plainPass.charAt(i);
            value++;
            encrypted[i] = (char)value;
        }
        
        return new String(encrypted);
    }

    /*
     * The decrypt function will substract the increased character from
     * the encrypt function.
     */
    @Override
    public String decrypt(String encryptedPass) {
        
        int total = encryptedPass.length();
        char[] decrypted = new char[total];
        for (int i = 0; i < total; i++) {
            int value = encryptedPass.charAt(i);
            value--;
            decrypted[i] = (char)value;
        }
        
        return new String(decrypted);
    }
    
}
