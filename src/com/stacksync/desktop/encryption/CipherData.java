/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

/**
 *
 * @author javigd
 */
public abstract class CipherData {
    private byte[] cipherText;
    
    public CipherData() {
        this.cipherText = null;
    }

    public CipherData(byte[] cipherText) {
        this.cipherText = cipherText;
    }
    
    public byte[] getCipherText() {
        return cipherText;
    }

    public void setCipherText(byte[] cipherText) {
        this.cipherText = cipherText;
    }
}
