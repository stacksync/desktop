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
package com.stacksync.desktop.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import org.apache.log4j.Logger;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Cristian Cotes <cotes.cristian@gmail.com>
 */
public class AdlerChecksum implements ChecksumCreator { 
    
    private static final Logger logger = Logger.getLogger(AdlerChecksum.class.getName());

    private Checksum check;
    
    public AdlerChecksum() { 
        this.check = new Adler32();
    }
    
    @Override
    public synchronized String createChecksum(byte[] data, int offset, int length) {
        check.reset();
        check.update(data, offset, length);
        return String.format("%020d", check.getValue());
    }

    @Override
    public Long getFileChecksum(File file) {
        
        long checksum = -1;
        byte[] buffer = new byte[512];

        try {
            FileInputStream fis = new FileInputStream(file);
            CheckedInputStream cis = new CheckedInputStream(fis, check);
            
            int read = cis.read(buffer);
            while(read != -1){
                read = cis.read(buffer);
            }
            
            fis.close();
            cis.close();

            checksum = check.getValue();
        } catch (IOException ex) {
            logger.error("Error getting file checksum: "+ex);
        }
        return checksum;
    }
}