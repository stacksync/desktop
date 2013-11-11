/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.chunker.TTTD;

import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.index.ChecksumCreator;
import com.stacksync.desktop.index.Sha1Checksum;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Cristian Cotes <cotes.cristian@gmail.com>
 */
public class TTTDChunker extends ChunkEnumeration {

    private static final Logger logger = Logger.getLogger(TTTDChunker.class.getSimpleName());
    
    private int Tmin;
    private int Tmax;
    private int D;
    private int Ddash;

    private RollingChecksum rolling;
    private ChecksumCreator checksumSHA1;
    
    public TTTDChunker(File file) throws FileNotFoundException {

        super(file);
        
        this.Tmin = 128*1024;
        this.Tmax = 256*1024;
        this.D = 540;
        this.Ddash = 270;

        this.rolling = new RollingChecksum();
        this.rolling.reset();
        
        checksumSHA1 = new Sha1Checksum();
    }

    @Override
    public boolean hasMoreElements() {
        if (closed) {
            return false;
        }

        try {
            return fis.available() > 0;
        }
        catch (IOException ex) {
            logger.warn("Error while reading from file input stream.", ex);
            return false;
        }
    }

    @Override
    public FileChunk nextElement() {
        if (closed) {
            return null;
        }

        try {
            // TTTD
            // Treat every round as new file       
            int p = 0; // no need for "l"; always zero
            int backupBreak = 0;

            int breakpoint = -1;

            byte[] c = new byte[1];
            byte[] buffer = new byte[Tmax];

            int read;
            for (; (read = cis.read(c)) != -1 ; p++) {
                buffer[p] = c[0];

                long hash = rolling.calculcateChecksum(c[0]);
                
                if (p < Tmin) {
                    // not at minimum size yet
                    continue;
                }


                if (Math.abs(hash % Ddash) == Ddash-1) {      
                    // possible backup break
                    backupBreak = p;     
                }

                if (Math.abs(hash % D) == D-1) {
                    // we found a breakpoint
                    // before the maximum threshold.
                    breakpoint = p;
                    break;
                }

                if (p < Tmax){
                    // we have failed to find a breakpoint,
                    // but we are not at the maximum yet
                    continue;
                }

                
                // when  we  reach  here,  we  have
                // not  found  a  breakpoint  with
                // the  main  divisor,  and  we  are
                // at  the  threshold.  If  there
                // is  a  backup  breakpoint,  use  it.
                // Otherwise  impose  a  hard  threshold.
                if (backupBreak != 0) {
                    breakpoint = backupBreak;
                    break;
                }
                else {
                    breakpoint = p;
                    break;
                }
            }

            // Close if this was the last bytes
            if (read == -1) {
                //fis.close();
                cis.close();
                closed = true;
                
                // TODO fix bug
                p--;
            }         

            // EOF as breakpoint
            if (breakpoint == -1) {
                breakpoint = p;
            }

            // Create chunk
            byte[] chunkContents = new byte[breakpoint+1];
            System.arraycopy(buffer, 0, chunkContents, 0, breakpoint+1);
            String chunkChecksum = checksumSHA1.createChecksum(chunkContents, 0, chunkContents.length);
            //long chunkChecksum = 0;
            long chunkNumber = number++;  

            return new FileChunk(chunkChecksum, chunkContents, chunkNumber, check.getValue(), breakpoint);//check.getValue());
        } 
        catch (IOException ex) {                
            logger.error("Error while retrieving next chunk.", ex);
            return null;
        }
    }
}
