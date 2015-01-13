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
package com.stacksync.desktop.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.stacksync.desktop.chunker.Chunker;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.util.FileUtil;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Cristian Cotes <cotes.cristian@gmail.com>
 */

public class TestChunker {
    
    private static final String FILES[] = {"/home/cotes/test/files/file500mb.dat"};//,
                                            /*"/home/cotes/test/files/file10mb.dat",
                                            "/home/cotes/test/files/file100mb.dat",
                                            "/home/cotes/test/files/file250mb.dat",
                                            "/home/cotes/test/files/file500mb.dat",
                                            "/home/cotes/test/files/file750mb.dat",
                                            "/home/cotes/test/files/file1000mb.dat",
                                            };*/
    
    private Chunker chunker = new Chunker();
    private static ArrayList<Long> timesTTTD = new ArrayList<Long>();
    private static ArrayList<Long> timesStatic = new ArrayList<Long>();
    
    public void createAndSaveChunks(String filePath, String chunkType) throws FileNotFoundException, IOException {
        
        File file = new File(filePath);
        
        ChunkEnumeration chunks = chunker.createChunks(file, chunkType);
        
        long start = System.currentTimeMillis();
        while (chunks.hasMoreElements()) {
            
            FileChunk chunk = chunks.nextElement();
            
            File destFile = new File("/home/cotes/test/files/dest_files_"+chunkType+"/"+chunk.getChecksum());
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            
            FileUtil.writeFile(chunk.getContents(), destFile);
            
        }
        long end = System.currentTimeMillis();
        
        if (chunkType.equals("static")) {
            timesStatic.add(end - start);
        } else if (chunkType.equals("TTTD")) {
            timesTTTD.add(end - start);
        }
        
    }
    
    @Test
    public void testTTTDChunker() {
        
        for(String filePath : FILES) {
            try {
                createAndSaveChunks(filePath, "TTTD");
            } catch (FileNotFoundException ex) {
                assertTrue(false);
            } catch (IOException ex) {
                assertTrue(false);
            }
        }
    }
    
    @Test
    public void testStaticChunker() {
        
        for(String filePath : FILES) {
            try {
                createAndSaveChunks(filePath, "static");
            } catch (FileNotFoundException ex) {
                assertTrue(false);
            } catch (IOException ex) {
                assertTrue(false);
            }
        }
    }
    
    @AfterClass
    public static void calculateTimes() {
        for (int i=0; i<FILES.length; i++) {
            long tStatic = timesStatic.get(i);
            long tTTTD = timesTTTD.get(i);
            System.out.println(tStatic + "\t" + tTTTD + "\t" + (tTTTD - tStatic));
        }
    }
    
}
