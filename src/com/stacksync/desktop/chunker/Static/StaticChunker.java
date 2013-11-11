/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.chunker.Static;

import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.index.ChecksumCreator;
import com.stacksync.desktop.index.Sha1Checksum;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class StaticChunker extends ChunkEnumeration{
    
    private static Logger logger = Logger.getLogger(StaticChunker.class.getName());
    
    public static final int CHUNK_SIZE = 512;
    
    private byte[] buffer;
    private ChecksumCreator checksumSHA1;
    
    public StaticChunker(File file) throws FileNotFoundException {
        super(file);
        checksumSHA1 = new Sha1Checksum();
    }

    @Override
    public boolean hasMoreElements() {
        if (closed) {
            return false;
        }

        try {
            return cis.available() > 0;
        } catch (IOException ex) {

            logger.warn("#Error while reading from file input stream.", ex);
            return false;
        }
    }

    @Override
    public FileChunk nextElement() {
        
        buffer = new byte[CHUNK_SIZE*1024];
        try {
            int read = cis.read(buffer);

            if (read == -1) {
                return null;
            }

            // Close if this was the last bytes
            if (cis.available() == 0) {
                cis.close();
                closed = true;
            }

            // Create chunk
            String chunkChecksum = checksumSHA1.createChecksum(buffer, 0, read);
            byte[] chunkContents = (read == buffer.length) ? buffer : Arrays.copyOf(buffer, read);
            long chunkNumber = number++;

            return new FileChunk(chunkChecksum, chunkContents, chunkNumber, check.getValue());
        } catch (IOException ex) {
            logger.error("#Error while retrieving next chunk.", ex);
            return null;
        }
    }
    
}
