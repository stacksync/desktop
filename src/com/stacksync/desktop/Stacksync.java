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
package com.stacksync.desktop;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.commons.cli.*;
import org.apache.log4j.extras.DOMConfigurator;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.error.ErrorDialog;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.util.StringUtil;
import java.net.MalformedURLException;


/**
 * Main class for the Stacksync client.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Stacksync {
    private static final Config config = Config.getInstance();
    private static final Environment env = Environment.getInstance();
    private static final CommandLineParser parser = new PosixParser();
    
    private static String[] args;

    private static void showHelp(Options options) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp("java -jar Stacksync.jar", options);
        System.exit(0);
    }
    
    private static Options createOptions(){
        // create the Options
        Options options = new Options();

        options.addOption("d", "daemon", false, "To use Stacksync with a daemon only.");
        options.addOption("c", "config", true, "Alternative path o config.xml file (Default: ~/.stacksync)" );
        options.addOption("ext", "extended", false, "To launch StackSync in an extended mode (more options are available to set up).");
        options.addOption("h", "help", false, "Print this message.");
        
        return options;
    }
    
    public static void start() {
        Boolean deamonMode = false;
        Boolean extendedMode;
        
        try {            
            try{
                File file = new File(env.getDefaultUserConfigDir() + File.separator + "conf" + File.separator + Constants.LOGGING_DEFAULT_FILENAME);
                DOMConfigurator.configure(file.toURI().toURL());
            } catch(NullPointerException e){
                System.out.println("No log4j config file was found no logs will be saved for this stacksync instance please make sure LogProperties.xml file is correctly placed " + e.toString());
            } catch (MalformedURLException ex) {
                System.out.println("No log4j config file was found no logs will be saved for this stacksync instance please make sure LogProperties.xml file is correctly placed " + ex.toString());
            }
            
            // create the command line parser
            Options options = createOptions();
            CommandLine line = parser.parse(options, args);

            // Help
            if (line.hasOption("help")) {
                showHelp(options);
            }
            
            deamonMode = line.hasOption("daemon");
            config.setDaemonMode(deamonMode);
            extendedMode = line.hasOption("extended");
            config.setExtendedMode(extendedMode);

            // Load config
            if (line.hasOption("config")) {
                File configFolder = new File(line.getOptionValue("config"));
                File configFile = new File(line.getOptionValue("config") + File.separator + "config.xml");
                
                if(configFolder.exists() && configFile.exists()){
                    config.load(configFolder);
                } else{
                    if(!configFolder.exists()){
                        throw new ConfigException("config folder " + configFolder + " doesn't exist.");
                    } else{
                        throw new ConfigException(configFile + " doesn't exist.");
                    }
                }
                
                if (config.getProfile() == null) {
                    throw new ConfigException("Could not load a profile, check the configuration file.");
                }                
            } else {
                
                File configurationDir = env.getAppConfDir();                
                if(configurationDir.exists()){
                    config.load();
                    if (config.getProfile() == null) {
                        File folder = new File(config.getConfDir()+File.separator+Constants.CONFIG_DATABASE_DIRNAME);                    
                        File configFile = new File(config.getConfDir()+File.separator+Constants.CONFIG_FILENAME);

                        folder.delete();
                        configFile.delete();
                        config.load();
                    } 
                } else { // new configuration
                    config.load();
                }
            }
            
        } catch (ConfigException e) {
            System.err.println("ERROR: Configuration exception: ");
            System.err.println(StringUtil.getStackTrace(e));
            System.exit(1);
        } catch (ParseException e) {
            System.err.println("ERROR: Command line arguments invalid: ");
            System.err.println(StringUtil.getStackTrace(e));
            System.exit(1);
        }

        // Start app!
        try {
            // TODO fixit
            //RemoteLogs.sendFailedLogs();
            Application appStacksync = new Application();
            appStacksync.start();
        } catch (Exception e) {
            if(!deamonMode){
                ErrorDialog.showDialog(e);
            }
        }
    }    
    
    private static void checkOrCreateLogFile(){
        
        //set the apache derby logs file location
        System.setProperty("derby.system.home", env.getDefaultUserConfigDir() + File.separator + Constants.CONFIG_DATABASE_DIRNAME); 
        
        File fileLogConfig = new File(env.getDefaultUserConfigDir() + File.separator + "conf" + File.separator + Constants.LOGGING_DEFAULT_FILENAME);        
        if(fileLogConfig.exists()){         
            try {
                String content = FileUtil.readFileToString(fileLogConfig);
                File file = new File(env.getDefaultUserConfigDir().getAbsolutePath() + File.separator + "logs");
                
                content = content.replace("REPLACED_BY_APPLICATION", file.toURI().toURL().getFile());
                FileUtil.writeFile(content, fileLogConfig);                
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }     
    }
    
    //check socket of get emblems is opened
    private static boolean isAlreadyRunning() {
        try {
            Socket clientSocket = new Socket("localhost", Constants.COMMANDSERVER_PORT);
            clientSocket.close();
            return true;
        } catch (UnknownHostException ex) {
            System.out.println("Error not found localhost. Exception: " + ex.getMessage());
            return false;
        } catch (IOException ex) {
            //System.out.println("Error with socket. Exception: " + ex.getMessage());
            return false;
        }
    }
    
    /**
     * @param args Command line arguments for the Stacksync client
     *             See '--help'
     */
    public static void main(String[] args) throws ConfigException, InitializationException {        
        if(isAlreadyRunning()){
            System.out.println("Stacksync is already running!!!");
        } else{
            checkOrCreateLogFile();

            Stacksync.args = args; // Required for restart
            Stacksync.start();
        }
    }    
}
