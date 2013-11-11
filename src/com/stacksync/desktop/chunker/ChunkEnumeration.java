/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.chunker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public abstract class ChunkEnumeration implements Enumeration<FileChunk> {
    
    private static Logger logger = Logger.getLogger(ChunkEnumeration.class.getName());

    protected FileInputStream fis;
    protected CheckedInputStream cis;
    protected boolean closed;
    protected long number;
    protected Checksum check;
    
    public ChunkEnumeration(File file) throws FileNotFoundException {
        this.check = new Adler32();

        this.fis = new FileInputStream(file);
        this.cis = new CheckedInputStream(fis, check);
        this.closed = false;
        this.number = 0;
    }

    public void closeStream() {
        try {
            this.cis.close();
            this.fis.close();
        } catch (IOException ex) {
            logger.error("Error closing stream: "+ex);
        }
        
    }
    
}
