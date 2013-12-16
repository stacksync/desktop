package com.stacksync.desktop.chunker;

import java.io.File;
import java.io.FileNotFoundException;
import com.stacksync.desktop.index.AdlerChecksum;
import com.stacksync.desktop.index.ChecksumCreator;
import com.stacksync.desktop.chunker.Static.StaticChunker;
import com.stacksync.desktop.chunker.TTTD.TTTDChunker;


/**
 *
 * @author Guillermo Guerrero
 * @author Cristian Cotes
 *
 */
public class Chunker {
    
    private ChecksumCreator fileChecksum;

    public Chunker() {
        fileChecksum = new AdlerChecksum();
    }
    
    public synchronized Long createFileChecksum(File file) throws  FileNotFoundException {
        return fileChecksum.getFileChecksum(file);
    }
    
    public synchronized ChunkEnumeration createChunks(File file) throws FileNotFoundException {
        return new StaticChunker(file);
        //return new TTTDChunker(file);
    }
    
    public synchronized ChunkEnumeration createChunks(File file, String type) throws FileNotFoundException {
        if (type.equals("static")) {
            return new StaticChunker(file);
        }
        else {
            return new TTTDChunker(file);
        }
    }
}
