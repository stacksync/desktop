/**
 * Syncany
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
import java.util.logging.Level;
import java.util.logging.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.rackspace.RackspaceConnection;
import com.stacksync.desktop.exceptions.LocalFileNotFoundException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.exceptions.StorageQuotaExcedeedException;
import com.stacksync.desktop.repository.files.RemoteFile;

/**
 *
 * @author pheckel
 */
public class TestSpeed {

    public static MeasureThread[] initThreads(final Connection c, int count, final int KB) throws InterruptedException {
        Thread[] initTM = new Thread[count];
        final MeasureThread[] threads = new MeasureThread[count];
        
        for (int i = 0; i < count; i++) {
            final int j = i;
            
            initTM[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    threads[j] = new MeasureThread(c, KB);
                }
            });
            
            initTM[i].start();
        }     
        
        for (int i = 0; i < count; i++) {
            initTM[i].join();
        }

        return threads;
    }
    
    private static class MeasureThread extends Thread {        
        private TransferManager tm;
        private int KB;
        private long duration;
        
        public MeasureThread(Connection c, int KB) {
            this.KB = KB;
            this.tm = c.createTransferManager();    
            
            try {
                tm.list(); // populate cache
            } catch (StorageException ex) {
                Logger.getLogger(TestSpeed.class.getName()).log(Level.SEVERE, null, ex);
            }
        }                

        @Override
        public void run() {
            try {        
                long startP = System.currentTimeMillis();

                tm.upload(new File("/home/pheckel/Syncany/Philipp's PC/Performance/" + KB), new RemoteFile("chunk" + Math.random()));

                long endP = System.currentTimeMillis();
                duration = (endP - startP);
            } 
            catch (LocalFileNotFoundException ex) {
                Logger.getLogger(TestSpeed.class.getName()).log(Level.SEVERE, null, ex);
            } catch (StorageException ex) {
                Logger.getLogger(TestSpeed.class.getName()).log(Level.SEVERE, null, ex);
            } catch (StorageQuotaExcedeedException ex) {
                Logger.getLogger(TestSpeed.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }    

    public static void main(String[] args) throws Exception {
        //enc();
        //dec();
        Config.getInstance().load();

        /*final PicasaConnection c = new PicasaConnection();
        c.setUsername("philipp.heckel");
        c.setPassword("...");
        c.setAlbumId("5606161375944250129");*/

        /*final LocalConnection c = new LocalConnection();
        c.setFolder(new File("/home/pheckel/Desktop/Philipp's PC/Performance/Repo"));*/

        /*final FtpConnection c = new FtpConnection();
        c.setHost("silversun.de");
        c.setUsername("stuff");
        c.setPassword("...");
        c.setPort(21);
        c.setPath("/syncany");*/
        /*
        final RackspaceConnection c = new RackspaceConnection();
        c.setUsername("binwiederhier");    
        c.setApiKey("...");
        c.setContainer("syncany-test");
         */
        final RackspaceConnection c = new RackspaceConnection();

        /*final SambaConnection c = new SambaConnection();
        c.setRoot("smb://localhost/share/private/");*/


        Thread.sleep(3000);
        
        int maxthread = 20;

        // KB
        for (int filesize = 64; filesize <= 1024; filesize += 32) {

            // THREADS
            //for (int threadCount = 4; threadCount <= 18; threadCount++) {
            
            // chunk size
            for (int chunksize = 4; chunksize <= 32; chunksize *=2) {
                if (chunksize >= filesize) {
                    continue;
                }
                
                int chunkCount = Math.round(filesize/chunksize);
                //int totalKB = threadCount*KB;
                              
                
                System.out.println("filesize = "+filesize+"; chunksize ="+chunksize);
                System.out.println("---");
                
                // SEQUENCE
            {
                MeasureThread[] threads = initThreads((Connection)c, chunkCount, chunksize);
                Thread.sleep(1000);
                
                System.out.print(chunkCount+" chunks in sequence ... ");
                long startP = System.currentTimeMillis();
                
                for (MeasureThread t : threads)  {
                    t.start();
                    t.join();
                }
                
                long endP = System.currentTimeMillis();
                long duration = (endP-startP);
                System.out.println(""+(endP-startP)+"ms -- "+((double)chunksize/((double)duration/1000/(double)threads.length))+" kb/s");
                //System.out.println();
                }
            
  // PARALLEL
                {
                    MeasureThread[] threads = initThreads((Connection)c, chunkCount, chunksize);
                    Thread.sleep(1000);

                    System.out.print(chunkCount+" chunks in parallel threads ... ");

                    long startP = System.currentTimeMillis();

                    for (MeasureThread t : threads) {
                        t.start();
                    }

                    for (MeasureThread t : threads) {
                        t.join();
                    }
                    

                    long endP = System.currentTimeMillis();
                    long duration = (endP - startP);
                    System.out.println("" + (endP - startP) + "ms -- " + ((double) chunksize / ((double) duration / 1000 / (double) threads.length)) + " kb/s");
                }

                // SINGLE FILE
                {
                    MeasureThread[] threads = initThreads((Connection)c, 1, filesize);
                    Thread.sleep(1000);

                    System.out.print("Upload as single file ('metachunk') ... ");

                    long startP = System.currentTimeMillis();

                    for (MeasureThread t : threads) {
                        t.start();
                    }

                    for (MeasureThread t : threads) {
                        t.join();
                    }
                    

                    long endP = System.currentTimeMillis();
                    long duration = (endP - startP);
                    System.out.println("" + (endP - startP) + "ms -- " + ((double) filesize / ((double) duration / 1000 / (double) threads.length)) + " kb/s");
                }             
                            
            
                System.out.println("------------------");
                System.out.println();
                
            
            }

            System.out.println();
            System.out.println("#########################################");
            System.out.println();
        }

        /*
        TransferManager tm = c.createTransferManager();
        tm.upload(new File("/home/pheckel/Coding/syncany/syncany/lib/plugins/picasa/guava-r09.jar"), new RemoteFile("chunk"));
        
        for (RemoteFile f : tm.list().values()) {
        System.out.println(f.getName());
        }
        
        tm.download(new RemoteFile("chunk"), new File("/home/pheckel/Desktop/Philipp's PC/Steno/testfile-tm-downloaded"));
         */
        //tm.delete(new RemoteFile("chunk"));
    }
}
