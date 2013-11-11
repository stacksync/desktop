/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.index;

import java.io.File;

/**
 *
 * @author cotes
 */
public interface ChecksumCreator {
    
    public String createChecksum(byte[] data, int offset, int length);
    
    public Long getFileChecksum(File file);
    
}
