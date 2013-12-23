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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.exceptions.CommandException;

/**
 *
 * @author Philipp C. Heckel
 */
public abstract class AbstractServer implements Runnable {

    protected final Logger logger = Logger.getLogger(AbstractServer.class.getName());
    protected final Config config = Config.getInstance();
    
    protected ServerSocket serverSocket;
    protected final List<AbstractWorker> workers;
    private List<Thread> workersThreads;
    protected final int port;
    protected boolean running;

    public AbstractServer(int port) {


        this.workers = new ArrayList<AbstractWorker>();
        this.workersThreads = new ArrayList<Thread>();
        this.serverSocket = null;
        this.port = port;
        this.running = false;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void setRunning(boolean running) {
        this.running = running;
        if (running == false) {
            for (int i=0; i<workersThreads.size(); i++) {
                Thread worker = workersThreads.get(i);
                worker.interrupt();
            }
            
            this.workersThreads = new ArrayList<Thread>();
            
            try {
                serverSocket.close();
            } catch (IOException ex) {
                
            }
        }
    }

    @Override
    public void run() {
        logger.debug("AbstractServer: Listening at localhost:" + port + " ...");

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            logger.debug("AbstractServer: Unable to bind server to port " + port + ". Address already in use?");
            return;
        }

        setRunning(true);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                AbstractWorker worker = createWorker(clientSocket);
                workers.add(worker);

                Thread workerThread = new Thread(worker, "AbstractWorker");
                workerThread.start();
                workersThreads.add(workerThread);
            } catch (IOException ex) {
                logger.debug("Client disconnected", ex);
            }
        }

        setRunning(false);
    }

    protected Map<String, List<String>> readArguments(BufferedReader in) throws CommandException, IOException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        List<String> argLines = new ArrayList<String>();

        // Read everything! The client must talk until 'done'!
        while (true) {
            String line = in.readLine();
            logger.debug("AbstractServer: Parameter: > " + line);

            if (line == null || line.startsWith("done")) {
                break;
            }

            argLines.add(line);
        }

        // Parse
        for (String line : argLines) {
            String[] parts = line.split("\t");

            if (parts.length == 0) {
                continue; // Ignore empty line!
            }

            if (parts.length == 1) {
                throw new CommandException("Invalid arguments: No value given for parameter '" + parts[0] + "'.");
            }

            String key = parts[0];
            List<String> values = new ArrayList<String>();

            for (int i = 1; i < parts.length; i++) {
                values.add(parts[i]);
            }

            result.put(key, values);
        }

        return result;
    }

    /*public void stop() {
        
        /*Iterator<Thread> it = workersThreads.iterator();
        while (it.hasNext()) {
            Thread worker = it.next();
            worker.interrupt();
        }*
        
        Thread worker = workersThreads.get(0);
        worker.interrupt();
        /*for (Thread worker : workersThreads) {
            worker.interrupt();
        }*
        this.workersThreads = new ArrayList<Thread>();
        
    }*/
    
    protected abstract AbstractWorker createWorker(Socket clientSocket);
}
