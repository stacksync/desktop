/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.ast.cloudABE.kpabe.CipherText;
import com.stacksync.commons.models.abe.ABEMetaComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author javigd
 */
public class AbeCipherData extends CipherData {

    private ArrayList<String> attributes;
    private HashMap<String, byte[]> encryptedAttributes;

    public AbeCipherData(byte[] cipherText, ArrayList<String> attributes,
            HashMap<String, byte[]> encrypted_attributes) {
        super(cipherText);
        this.attributes = attributes;
        this.encryptedAttributes = encrypted_attributes;
    }
    
    public AbeCipherData(byte[] cipherText, List<ABEMetaComponent> metaComponents) {
        super(cipherText);
        ArrayList<String> attrs = new ArrayList<String>();
        HashMap<String, byte[]> encryptedAttrs = new HashMap<String, byte[]>();
        for (ABEMetaComponent meta : metaComponents){
            String attribute = meta.getAttributeId();
            attrs.add(attribute);
            encryptedAttrs.put(attribute, meta.getEncryptedPKComponent());
        }
        this.attributes = attrs;
        this.encryptedAttributes = encryptedAttrs;
    }
    
    public AbeCipherData(CipherText cipher) {
        super(cipher.getEncrypted_message());
        this.attributes = cipher.getAttributes();
        this.encryptedAttributes = cipher.getEncrypted_attributes();
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<String> attributes) {
        this.attributes = attributes;
    }

    public HashMap<String, byte[]> getEncryptedAttributes() {
        return encryptedAttributes;
    }

    public void setEncrypted_attributes(HashMap<String, byte[]> encryptedAttributes) {
        this.encryptedAttributes = encryptedAttributes;
    }
    
    public ArrayList<com.stacksync.desktop.db.models.ABEMetaComponent> getAbeMetaComponents() {
        ArrayList<com.stacksync.desktop.db.models.ABEMetaComponent> metaComponents = 
                new ArrayList<com.stacksync.desktop.db.models.ABEMetaComponent>();
        
        for(String attribute : this.attributes) {
            metaComponents.add(new com.stacksync.desktop.db.models.ABEMetaComponent(
                    null, attribute, encryptedAttributes.get(attribute), null));
        }
        return metaComponents;
    }
    
    public CipherText toCipherText() {
        return new CipherText(this.getAttributes(), this.getCipherText(), this.getEncryptedAttributes());
    }

}
