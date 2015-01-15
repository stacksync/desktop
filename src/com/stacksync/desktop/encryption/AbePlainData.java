/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import java.util.ArrayList;

/**
 *
 * @author javigd
 */
public class AbePlainData extends PlainData {

    private ArrayList<String> attributes;
    
    public AbePlainData() {
        super();
    }
    
    public AbePlainData(byte[] data, ArrayList<String> attributes) {
        super(data);
        this.attributes = attributes;
    }
    
    public ArrayList<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<String> attributes) {
        this.attributes = attributes;
    }

}
