/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.chunker;

/**
 *
 * @author cotes
 */
public class FileChunk {
    private String checksum;
    private byte[] contents;
    private long number;
    private long fileChecksum;
    private long size;

    public FileChunk(String checksum, byte[] contents, long number) {
        this(checksum, contents, number, 0);
    }

    public FileChunk(String checksum, byte[] contents, long number, long fileChecksum) {
        this.checksum = checksum;
        this.contents = contents;
        this.number = number;
        this.fileChecksum = fileChecksum;
    }
    
    public FileChunk(String checksum, byte[] contents, long number, long fileChecksum, long size) {
        this.checksum = checksum;
        this.contents = contents;
        this.size = size;
        this.number = number;
        this.fileChecksum = fileChecksum;
    } 

    public String getChecksum() {
        return checksum;
    }

    public byte[] getContents() {
        return contents;
    }

    public long getNumber() {
        return number;
    }

    public long getFileChecksum() {
        return fileChecksum;
    }
    
    public long getSize() {
        return size;
    }
    
}
