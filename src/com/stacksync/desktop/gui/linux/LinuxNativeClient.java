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
package com.stacksync.desktop.gui.linux;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.apache.log4j.extras.DOMConfigurator;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.exceptions.TrayException;
import com.stacksync.desktop.gui.error.ErrorDialog;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.tray.Tray.StatusIcon;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.StringUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class LinuxNativeClient {

    private static final Environment env = Environment.getInstance();
    private static final Config config = Config.getInstance();
    private final Logger logger = Logger.getLogger(LinuxNativeClient.class.getName());
    //private final Logger serviceLogger = Logger.getLogger("PythonScript");
    private static ResourceBundle resourceBundle;
    private static final LinuxNativeClient instance = new LinuxNativeClient();
    /**
     * Send No-operation request every x milliseconds. Must be lower than
     * {@link LinuxNativeService#TIMEOUT_BEFORE_EXIT}.
     */
    private static final int NOP_INTERVAL = 5000;
    private static final int RETRY_COUNT = 3;
    private static final int RETRY_SLEEP = 50;
    private boolean initialized;
    private boolean terminated;
    private boolean active;
    private Process serviceProcess;
    private int servicePort;
    private BufferedReader serviceIn;

    private LinuxNativeClient() {

        initialized = false;
        terminated = false;
        resourceBundle = config.getResourceBundle();
    }

    public static synchronized LinuxNativeClient getInstance() {
        return instance;
    }

    public synchronized void init(String initialMessage) throws InitializationException {
        if (initialized) {
            return;
        }

        active = true;
        startService(initialMessage);
        startNopThread();
        initialized = true;
        try {
            // Set first icon
            send(new UpdateStatusIconRequest(StatusIcon.DISCONNECTED));
        } catch (TrayException ex) { }
    }

    public synchronized void destroy() {
        terminated = true;

        if (serviceProcess != null) {
            serviceProcess.destroy();
        }
    }
    
    public boolean isActive() {
        return this.active;
    }

    public Object send(Request request) throws TrayException {
        for (int i = 1; i <= RETRY_COUNT; i++) {
            
            PrintWriter out = null;
            InputStreamReader isr = null;
            BufferedReader in = null;
            
            try {
                Socket socket = connect();
                out = new PrintWriter(socket.getOutputStream());
                isr = new InputStreamReader(socket.getInputStream());
                in = new BufferedReader(isr);

                // Request
                out.print(request + "\n");
                out.flush();
                logger.debug("Sent request " + request + ". Waiting for response ...");

                // Response
                Object response = request.parseResponse(in.readLine());
                logger.debug("Received response: " + response);

                in.close();
                out.close();                
                socket.close();
                
                return response;
            } catch (Exception ex) {
                if (i < RETRY_COUNT) {
                    logger.warn("Could not send request " + request + " to native server. RETRYING ...", ex);

                    try {
                        Thread.sleep(RETRY_SLEEP);
                    } catch (InterruptedException e2) { }
                    continue;
                } else {
                    throw new TrayException("Tray error send exception: ", ex);
                }
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

        // FAILED.
        logger.error("Could not send request "+request+" to native server. RETRAYING FAILED!");     
        return null;
    }

    private Socket connect() throws IOException {
        Socket connection = new Socket();

        // Re-connect
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                connection.connect(new InetSocketAddress("localhost", servicePort), 1000);
                logger.debug("Connected to native server on port " + servicePort);
                return connection;
            } catch (IOException e) {
                if (i < RETRY_COUNT) {
                    logger.warn("Cannot connect to native server on port " + servicePort + ". RETRYING ...", e);

                    try {
                        Thread.sleep(RETRY_SLEEP);
                    } catch (InterruptedException e2) { }
                    
                    continue;
                }
            }
        }

        // FAILED.
        logger.error("Cannot connect to native server. Retrying failed!");
        IOException ex = new IOException("Unable to connect to service on port " + servicePort);
        RemoteLogs.getInstance().sendLog(ex);
        throw ex;
    }

    public static void main(String[] args) throws ConfigException, InitializationException, InterruptedException {
        //for (Entry<Object, Object> entry : System.getProperties().entrySet()) 
        //  System.out.println(entry.getKey() + " = "+entry.getValue());

        try {
            try{
                File file = new File(env.getDefaultUserConfigDir() + File.separator + Constants.LOGGING_DEFAULT_FILENAME);
                DOMConfigurator.configure(file.toURI().toURL());
            } catch(NullPointerException e){
                System.out.println("No log4j config file was found no logs will be saved for this stacksync instance please make sure LogProperties.xml file is correctly placed " + e.toString());
            } catch (MalformedURLException ex) {
                System.out.println("No log4j config file was found no logs will be saved for this stacksync instance please make sure LogProperties.xml file is correctly placed " + ex.toString());
            }
            
            config.load();
            Tray tray = Tray.getInstance();
            tray.registerProcess(getInstance().getClass().getSimpleName());
            //LinuxNativeClient.getInstance().init();
            //Object send = LinuxNativeClient.getInstance().send(new BrowseFileRequest(BrowseFileRequest.BrowseType.FILES_ONLY));

            tray.init("Everything is up to date.");
            tray.notify("Stacksync", "Sending my regards", new File("/home/pheckel/Coding/stacksync/stacksync/res/logo48.png"));
            tray.setStatusIcon(getInstance().getClass().getSimpleName(), StatusIcon.UPDATING);
            tray.setStatusText(getInstance().getClass().getSimpleName(), "hello!");
            
            Thread.sleep(1000);
            tray.setStatusIcon(getInstance().getClass().getSimpleName(), StatusIcon.DISCONNECTED);
            Thread.sleep(1000);
            tray.setStatusIcon(getInstance().getClass().getSimpleName(), StatusIcon.UPDATING);
            Thread.sleep(1000);
            tray.setStatusIcon(getInstance().getClass().getSimpleName(), StatusIcon.UPTODATE);
            Thread.sleep(1000);
            tray.updateUI();

            int i = 1;
            while (true) {
                Thread.sleep(1000);

                tray.setStatusText(getInstance().getClass().getSimpleName(), 
                        resourceBundle.getString("lnc_downloading_files") + " 1/20 ...");
                Thread.sleep(1000);
                tray.setStatusText(getInstance().getClass().getSimpleName(),
                        resourceBundle.getString("lnc_downloading_files") + " 2/20 ...");
                Thread.sleep(1000);
                tray.setStatusText(getInstance().getClass().getSimpleName(),
                        resourceBundle.getString("lnc_downloading_files") + " 3/20 ...");
                Thread.sleep(1000);
                tray.setStatusText(getInstance().getClass().getSimpleName(),
                        resourceBundle.getString("lnc_downloading_files") + " 4/20 ...");

                Thread.sleep(1000);

                Profile profile = new Profile();
                profile.setName("Profile " + (i++));
                profile.setFolder(new Folder(profile));
                Folder folder = new Folder(profile);
                folder.setLocalFile(new File("/home"));
                config.setProfile(profile);
                tray.updateUI();
            }

        } finally {
            //NativeClient.getNativeClient().destroy();
        }

    }

    private void startService(String initialMessage) throws InitializationException {
        try {
            // Path to executable (python2 for arch linux bug #793524) and script
            String pythonBinary = (new File("/usr/bin/python2").exists()) ? "/usr/bin/python2" : "/usr/bin/python";
            String nativeScript = env.getAppBinDir() + File.separator + "native.py";
            //ProcessBuilder builder = new ProcessBuilder(pythonBinary, nativeScript, config.getResDir().getAbsolutePath(), initialMessage);
            ProcessBuilder builder = new ProcessBuilder(pythonBinary, nativeScript, "", initialMessage);

            builder.redirectErrorStream(true);
            logger.debug("Starting LinuxNativeService : " + builder.command());

            // Start the server process
            servicePort = 0;
            serviceProcess = builder.start();
            serviceIn = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()));

            // Read the port (one-line)
            String line;
            List<String> errors = new ArrayList<String>();

            while (servicePort == 0) {
                try {
                    line = serviceIn.readLine();
                } catch (IOException e) {
                    ErrorDialog.showDialog(new Exception("ERROR: Could not read from native script:\n" + StringUtil.getStackTrace(e)));
                    return;
                }

                if (line == null) {
                    break;
                }

                // Parse port
                if (line.startsWith("PORT=")) {
                    servicePort = Integer.parseInt(line.substring("PORT=".length()));
                    break;
                }

                // Add python error/warning
                errors.add(line);
            }

            // Print errors and warnings
            for (String errline : errors) {
                logger.error("Python script error/warning: " + errline);
            }

            if (servicePort == 0) {
                if (errors.isEmpty()) {
                    ErrorDialog.showDialog(new Exception("PYTHON SCRIPT ERROR: Unable to launch script '" + nativeScript + "'. No warnings or errors?!"));
                } else {
                    ErrorDialog.showDialog(new Exception("PYTHON SCRIPT ERROR:\n" + StringUtil.join(errors, "\n")));
                }

                return;
            }

            // Catch and redirect everything coming from the server
            // send it to the logger.

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;

                        while ((line = serviceIn.readLine()) != null) {
                            logger.debug("" + line);
                        }
                    } catch (IOException ex) {
                        logger.warn("TRAY SERVICE TERMINATED.", ex);
                    }
                }
            }, "TrayServRead").start();


            Thread.sleep(1000); // TODO do we need this?
        } catch (Exception e) {
            throw new InitializationException(e);
        }
    }

    private void startNopThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                int attempts = 0;
                while (!terminated) {
                    
                    try {
                        send(new NopRequest());
                        attempts = 0;
                        active = true;
                    } catch (TrayException ex) {
                        if (active) {
                            logger.warn(ex);
                            attempts++;
                            if (attempts >= RETRY_COUNT) {
                                active = false;
                            }
                        }

                    }
                    
                    try {
                        Thread.sleep(NOP_INTERVAL);
                    } catch (InterruptedException ex) {
                        logger.warn("TRAY SERVICE TERMINATED.", ex);
                    }

                }
            }
        }, "NativeNOPThread").start();
    }
}
