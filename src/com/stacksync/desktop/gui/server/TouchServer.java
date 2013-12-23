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
package com.stacksync.desktop.gui.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.stacksync.desktop.logging.RemoteLogs;

/**
 *
 * @author Philipp C. Heckel
 */
public class TouchServer extends AbstractServer
        implements Runnable /* THIS MUST BE HERE. Otherwise the thread won't start! */ {

    public TouchServer() {
        super(32587);
    }

    public void touch(File file) {
        if (workers.isEmpty()) {
            logger.debug("Cannot touch file. No touch workers available.");
            return;
        }
        logger.debug("TouchServer: Touch " + file);

        synchronized (workers) {
            for (AbstractWorker worker : workers) {
                logger.debug("TouchServer: Sending shell touch to client of " + worker);

                // Touch it, baby!
                ((TouchWorker) worker).touch(file);
            }
        }
    }

    @Override
    protected AbstractWorker createWorker(Socket clientSocket) {
        return new TouchWorker(clientSocket);
    }

    private class TouchWorker extends AbstractWorker {

        private final BlockingQueue<File> queue;

        public TouchWorker(Socket clientSocket) {
            super(clientSocket);
            queue = new LinkedBlockingQueue<File>();
        }

        private void touch(File file) {
            queue.add(file);
        }

        @Override
        public void run() {
            logger.debug("TouchWorker: Client connected.");

            PrintWriter out = null;
            BufferedReader in = null;
            InputStreamReader isr = null;
            
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                isr = new InputStreamReader(clientSocket.getInputStream());
                in = new BufferedReader(isr);

                try {
                    while (true) {
                        File touchFile = queue.take();

                        if (touchFile == null) {
                            break;
                        }
                        logger.debug("TouchWorker: Sending touch " + touchFile + " ... ");

                        out.print("shell_touch\n");
                        out.print("path\t" + touchFile.getAbsolutePath() + "\n");
                        out.print("done\n");
                        out.flush();
                    }
                } catch (InterruptedException ex) {
                    logger.error("TouchWorker got interrupted. TERMINATING.", ex);
                    RemoteLogs.getInstance().sendLog(ex);
                    return;
                }

                clientSocket.close();
            } catch (IOException e) {
                logger.debug("Socket error in TouchWorker.", e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }

                    if(isr != null){
                        isr.close();
                    }
                    
                    if (in != null) {
                        in.close();
                    }                    
                } catch (IOException ex) {
                    logger.debug("I/O Exception.", ex);
                }
            }
        }
    }
}